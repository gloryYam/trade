# ARCHITECTURE.md

## 시스템 아키텍처 상세 설계

이 문서는 실시간 암호화폐 모의투자 플랫폼의 상세 아키텍처 설계를 다룹니다.

---

## 목차
1. [전체 시스템 아키텍처](#전체-시스템-아키텍처)
2. [실시간 가격 수집 시스템](#실시간-가격-수집-시스템)
3. [Redis 캐싱 전략](#redis-캐싱-전략)
4. [분산 락 설계](#분산-락-설계)
5. [이벤트 드리븐 아키텍처](#이벤트-드리븐-아키텍처)
6. [보안 아키텍처](#보안-아키텍처)
7. [성능 최적화 전략](#성능-최적화-전략)
8. [확장성 고려사항](#확장성-고려사항)

---

## 전체 시스템 아키텍처

### 계층 구조

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Layer                            │
│                   (Web / Mobile App)                         │
└─────────────────────────────────────────────────────────────┘
                            ↓ HTTPS/WSS
┌─────────────────────────────────────────────────────────────┐
│                   API Gateway Layer                          │
│            (Load Balancer + Rate Limiting)                   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                 Application Layer (Spring Boot)              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Auth Service │  │ Order Service│  │Market Service│      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│  ┌──────────────┐  ┌──────────────┐                        │
│  │Account Svc   │  │Position Svc  │                        │
│  └──────────────┘  └──────────────┘                        │
└─────────────────────────────────────────────────────────────┘
                ↓                    ↓                  ↓
┌──────────────────┐   ┌──────────────────┐   ┌──────────────┐
│  PostgreSQL/H2   │   │  Redis Cluster   │   │  MQ (Kafka)  │
│  (Persistent DB) │   │  (Cache + Lock)  │   │  (Events)    │
└──────────────────┘   └──────────────────┘   └──────────────┘
                                    ↑
                            ┌───────────────┐
                            │ Binance WS    │
                            │ (Price Feed)  │
                            └───────────────┘
```

### 컴포넌트 간 통신

| From | To | Protocol | Purpose |
|------|-----|----------|---------|
| Client | API Gateway | HTTPS | REST API 호출 |
| API Gateway | Spring Boot | HTTP | 요청 라우팅 |
| Spring Boot | PostgreSQL | JDBC | 영구 데이터 저장 |
| Spring Boot | Redis | TCP | 캐싱 + 분산 락 |
| Spring Boot | Kafka | TCP | 이벤트 발행/구독 |
| Binance | Market Service | WebSocket | 실시간 가격 수신 |

---

## 실시간 가격 수집 시스템

### Binance WebSocket 연동

#### WebSocket 클라이언트 구조

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class BinanceBookTickerWsClient {
    private final WebSocketClient webSocketClient;
    private final LatestPriceRedisStore redisStore;
    private final ObjectMapper objectMapper;

    private WebSocketSession session;
    private final List<String> symbols = List.of("btcusdt", "ethusdt", "bnbusdt");

    @PostConstruct
    public void connect() {
        String url = buildWebSocketUrl();
        WebSocketHandler handler = new BinanceWebSocketHandler();

        try {
            session = webSocketClient.doHandshake(handler, url).get();
            log.info("Binance WebSocket 연결 성공: {}", url);
        } catch (Exception e) {
            log.error("WebSocket 연결 실패", e);
            scheduleReconnect();
        }
    }

    private String buildWebSocketUrl() {
        String streams = symbols.stream()
            .map(symbol -> symbol.toLowerCase() + "@bookTicker")
            .collect(Collectors.joining("/"));
        return "wss://stream.binance.com:9443/stream?streams=" + streams;
    }

    private class BinanceWebSocketHandler extends TextWebSocketHandler {
        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            try {
                BookTickerMessage ticker = objectMapper.readValue(
                    message.getPayload(),
                    BookTickerMessage.class
                );

                PriceTick priceTick = BookTickerMessageMapper.toDomain(ticker);
                redisStore.saveLatestPrice(priceTick);

                log.debug("가격 업데이트: {} = {}", priceTick.symbol(), priceTick.price());
            } catch (Exception e) {
                log.error("메시지 처리 실패: {}", message.getPayload(), e);
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            log.warn("WebSocket 연결 종료: {}", status);
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        // 5초 후 재연결 시도
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                connect();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}
```

#### 메시지 포맷

**Binance bookTicker 스트림:**
```json
{
  "stream": "btcusdt@bookTicker",
  "data": {
    "u": 400900217,
    "s": "BTCUSDT",
    "b": "25260.00000000",
    "B": "31.65000000",
    "a": "25260.01000000",
    "A": "40.33000000"
  }
}
```

**내부 도메인 모델 (PriceTick):**
```java
public record PriceTick(
    String symbol,          // "BTCUSDT"
    BigDecimal bidPrice,    // 매수 호가
    BigDecimal bidQty,      // 매수 수량
    BigDecimal askPrice,    // 매도 호가
    BigDecimal askQty,      // 매도 수량
    long updateId,          // 업데이트 ID
    Instant timestamp       // 수신 시각
) {}
```

### 재연결 전략

- **초기 연결 실패:** 5초 간격으로 재시도 (최대 10회)
- **연결 끊김:** 즉시 재연결 시도
- **하트비트:** 30초마다 ping 전송, 60초 무응답 시 재연결
- **백오프 전략:** 연속 실패 시 지수 백오프 적용 (5s → 10s → 20s → 40s)

---

## Redis 캐싱 전략

### 캐시 구조

#### 1. 최신 가격 캐시

**Key Pattern:** `price:latest:{symbol}`

**Value:** JSON String (PriceTick)

**TTL:** 60초 (1분)

```java
@Component
@RequiredArgsConstructor
public class LatestPriceRedisStore {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "price:latest:";
    private static final Duration TTL = Duration.ofSeconds(60);

    public void saveLatestPrice(PriceTick priceTick) {
        String key = KEY_PREFIX + priceTick.symbol();
        String value = objectMapper.writeValueAsString(priceTick);

        redisTemplate.opsForValue().set(key, value, TTL);
    }

    public Optional<PriceTick> getLatestPrice(String symbol) {
        String key = KEY_PREFIX + symbol;
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return Optional.empty();
        }

        return Optional.of(objectMapper.readValue(value, PriceTick.class));
    }
}
```

#### 2. 사용자 세션 캐시

**Key Pattern:** `session:{userId}`

**Value:** JWT Payload JSON

**TTL:** 3600초 (1시간)

#### 3. 계좌 잔고 캐시

**Key Pattern:** `account:balance:{accountId}`

**Value:** BigDecimal String

**TTL:** 300초 (5분)

**캐시 무효화 전략:**
- Write-Through: 주문 체결 시 즉시 캐시 업데이트
- 트랜잭션 성공 후 캐시 갱신
- 실패 시 캐시 삭제 (DB를 Source of Truth로)

### Redis 설정

```yaml
# application.yml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 10
        max-idle: 5
        min-idle: 2
      shutdown-timeout: 2000ms
```

---

## 분산 락 설계

### Redisson 기반 분산 락

#### 의존성 추가 (예정)

```gradle
implementation 'org.redisson:redisson-spring-boot-starter:3.24.3'
```

#### 분산 락 구현

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockManager {
    private final RedissonClient redissonClient;

    /**
     * 분산 락 획득 및 작업 실행
     *
     * @param lockKey 락 키 (예: "order:lock:accountId:123")
     * @param waitTime 락 대기 시간 (초)
     * @param leaseTime 락 자동 해제 시간 (초)
     * @param task 실행할 작업
     */
    public <T> T executeWithLock(
        String lockKey,
        long waitTime,
        long leaseTime,
        Supplier<T> task
    ) {
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);

            if (!acquired) {
                throw new LockAcquisitionException(
                    "락 획득 실패: " + lockKey + " (다른 요청 처리 중)"
                );
            }

            log.debug("락 획득 성공: {}", lockKey);
            return task.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrderProcessingException("락 획득 중단", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("락 해제: {}", lockKey);
            }
        }
    }
}
```

#### 주문 처리 시 분산 락 적용

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final DistributedLockManager lockManager;
    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;

    public Order createOrder(Long accountId, OrderRequest request) {
        String lockKey = "order:lock:account:" + accountId;

        return lockManager.executeWithLock(
            lockKey,
            3,  // 3초 대기
            10, // 10초 후 자동 해제
            () -> processOrder(accountId, request)
        );
    }

    @Transactional
    private Order processOrder(Long accountId, OrderRequest request) {
        // 1. 계좌 조회 (비관적 락)
        Account account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));

        // 2. 잔고 확인
        BigDecimal requiredAmount = request.price().multiply(request.quantity());
        if (account.getBalance().compareTo(requiredAmount) < 0) {
            throw new InsufficientBalanceException(requiredAmount, account.getBalance());
        }

        // 3. 주문 생성
        Order order = Order.builder()
            .accountId(accountId)
            .symbol(request.symbol())
            .type(request.type())
            .price(request.price())
            .quantity(request.quantity())
            .status(OrderStatus.FILLED)
            .build();

        // 4. 잔고 차감
        account.deductBalance(requiredAmount);

        // 5. 저장
        orderRepository.save(order);
        accountRepository.save(account);

        return order;
    }
}
```

### 락 타임아웃 설정 가이드

| 작업 유형 | Wait Time | Lease Time | 비고 |
|----------|-----------|------------|------|
| 주문 생성 | 3초 | 10초 | 일반적인 주문 처리 |
| 대량 주문 | 5초 | 20초 | 여러 종목 동시 주문 |
| 포지션 정산 | 10초 | 30초 | 복잡한 계산 필요 |
| 계좌 이체 | 5초 | 15초 | 이중 락 필요 |

### 데드락 방지 전략

1. **락 순서 일관성 유지:**
   - 항상 accountId 오름차순으로 락 획득
   - 여러 리소스 락 필요 시 정렬 후 순차 획득

2. **타임아웃 설정:**
   - 모든 락에 명시적 타임아웃 설정
   - Lease Time으로 자동 해제 보장

3. **락 범위 최소화:**
   - DB 트랜잭션과 분산 락 범위 일치
   - 락 획득 후 최소한의 작업만 수행

---

## 이벤트 드리븐 아키텍처

### Spring Events (내부 이벤트)

#### 주문 체결 이벤트

```java
public record OrderFilledEvent(
    Long orderId,
    Long accountId,
    String symbol,
    OrderType type,
    BigDecimal price,
    BigDecimal quantity,
    Instant filledAt
) {}
```

#### 이벤트 발행

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order executeOrder(OrderRequest request) {
        Order order = processOrder(request);

        // 트랜잭션 커밋 후 이벤트 발행
        eventPublisher.publishEvent(new OrderFilledEvent(
            order.getId(),
            order.getAccountId(),
            order.getSymbol(),
            order.getType(),
            order.getPrice(),
            order.getQuantity(),
            order.getFilledAt()
        ));

        return order;
    }
}
```

#### 이벤트 리스너

```java
@Component
@Slf4j
public class OrderEventListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderFilled(OrderFilledEvent event) {
        log.info("주문 체결: orderId={}, symbol={}, price={}",
            event.orderId(), event.symbol(), event.price());

        // 1. 포지션 업데이트
        updatePosition(event);

        // 2. 알림 전송
        sendNotification(event);

        // 3. 통계 업데이트
        updateStatistics(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCancelled(OrderCancelledEvent event) {
        // 취소 후처리
    }
}
```

### Kafka 연동 (향후 확장)

#### 토픽 설계

- `trade.order.created` - 주문 생성
- `trade.order.filled` - 주문 체결
- `trade.order.cancelled` - 주문 취소
- `trade.position.updated` - 포지션 변경
- `trade.account.deposited` - 입금
- `trade.account.withdrawn` - 출금

#### Producer 설정

```java
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, OrderFilledEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, OrderFilledEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

---

## 보안 아키텍처

### JWT 인증 흐름

```
[Client]
    ↓ POST /api/auth/login { username, password }
[AuthController]
    ↓
[AuthService] - 비밀번호 검증 (BCrypt)
    ↓
[JwtTokenProvider] - JWT 생성
    ↓ Token 발급
[Client] - Authorization: Bearer {token} 저장
    ↓
[Client] - GET /api/orders (+ Bearer Token)
    ↓
[JwtAuthenticationFilter] - Token 검증
    ↓ SecurityContext 설정
[OrderController] - @AuthenticationPrincipal로 사용자 정보 접근
```

### JWT 구조

```java
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration; // 3600000 (1시간)

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userDetails.getUsername());
        claims.put("roles", userDetails.getAuthorities());

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

### 보안 설정

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // strength 12
    }
}
```

---

## 성능 최적화 전략

### 1. 데이터베이스 최적화

#### 인덱스 전략

```sql
-- 주문 조회 최적화
CREATE INDEX idx_order_account_created ON orders(account_id, created_at DESC);
CREATE INDEX idx_order_symbol_status ON orders(symbol, status);

-- 포지션 조회 최적화
CREATE INDEX idx_position_account_symbol ON positions(account_id, symbol);

-- 계좌 조회 최적화
CREATE INDEX idx_account_user ON accounts(user_id);
```

#### N+1 문제 해결

```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ❌ N+1 발생
    List<Order> findByAccountId(Long accountId);

    // ✅ fetch join으로 해결
    @Query("SELECT o FROM Order o JOIN FETCH o.account WHERE o.accountId = :accountId")
    List<Order> findByAccountIdWithAccount(@Param("accountId") Long accountId);
}
```

#### 쿼리 최적화

```java
// Projection으로 필요한 컬럼만 조회
public interface OrderSummary {
    Long getId();
    String getSymbol();
    BigDecimal getPrice();
    OrderStatus getStatus();
}

@Query("SELECT o.id as id, o.symbol as symbol, o.price as price, o.status as status " +
       "FROM Order o WHERE o.accountId = :accountId")
List<OrderSummary> findOrderSummariesByAccountId(@Param("accountId") Long accountId);
```

### 2. 캐싱 전략

#### 다층 캐시 구조

```
[Application]
    ↓
[L1 Cache: Caffeine] - 로컬 메모리 캐시 (100ms TTL)
    ↓ (miss)
[L2 Cache: Redis] - 분산 캐시 (60s TTL)
    ↓ (miss)
[Database: PostgreSQL] - Source of Truth
```

#### Caffeine 로컬 캐시

```java
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(100, TimeUnit.MILLISECONDS)
            .recordStats());
        return cacheManager;
    }
}
```

### 3. 비동기 처리

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
```

### 4. 커넥션 풀 튜닝

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 3000
      idle-timeout: 600000
      max-lifetime: 1800000
```

---

## 확장성 고려사항

### 수평 확장 (Scale-Out)

#### 무상태 애플리케이션
- 세션을 Redis에 저장하여 서버 간 공유
- JWT 인증으로 상태 비저장
- 어떤 인스턴스에서도 동일한 응답 보장

#### 로드 밸런싱
```
[Nginx/ALB]
    ↓ (Round Robin)
[App Server 1] [App Server 2] [App Server 3]
    ↓              ↓              ↓
       [Shared Redis Cluster]
       [Shared PostgreSQL]
```

### 수직 확장 (Scale-Up)

- CPU: 주문 처리, 가격 계산
- Memory: Redis 캐시 크기
- Disk I/O: PostgreSQL 쿼리 성능

### 데이터베이스 샤딩 (향후)

- accountId 기반 샤딩
- 사용자 데이터 분산 저장
- Cross-shard 쿼리 최소화

---

## 모니터링 및 관측성

### 메트릭 수집

- **주문 처리 속도:** TPS (Transactions Per Second)
- **API 응답 시간:** P50, P95, P99 latency
- **캐시 히트율:** Redis 히트/미스 비율
- **WebSocket 연결 상태:** 활성 연결 수, 재연결 횟수
- **락 대기 시간:** 분산 락 획득 지연

### 로깅 전략

```java
@Slf4j
public class OrderService {

    public Order createOrder(OrderRequest request) {
        MDC.put("accountId", request.accountId().toString());
        MDC.put("symbol", request.symbol());

        try {
            log.info("주문 생성 시작: type={}, price={}, qty={}",
                request.type(), request.price(), request.quantity());

            Order order = processOrder(request);

            log.info("주문 체결 완료: orderId={}", order.getId());
            return order;

        } catch (Exception e) {
            log.error("주문 실패", e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
```

### Health Check

```java
@RestController
@RequestMapping("/actuator/health")
public class HealthCheckController {

    @GetMapping
    public ResponseEntity<HealthStatus> health() {
        return ResponseEntity.ok(new HealthStatus(
            checkDatabase(),
            checkRedis(),
            checkWebSocket()
        ));
    }
}
```

---

## 기술 의사결정 기록 (ADR)

### ADR-001: Redis vs Hazelcast (분산 캐시)
- **결정:** Redis 선택
- **이유:**
  - 업계 표준, 풍부한 생태계
  - Redisson으로 분산 락 쉽게 구현
  - Spring Boot 자동 설정 지원
- **트레이드오프:** 별도 인프라 필요 (Hazelcast는 임베디드 가능)

### ADR-002: Kafka vs RabbitMQ (메시지 큐)
- **결정:** Kafka 우선 검토, 초기에는 Spring Events
- **이유:**
  - Kafka: 높은 처리량, 이벤트 소싱 가능
  - 초기에는 Spring Events로 시작해 복잡도 감소
- **마이그레이션 경로:** Spring Events → Kafka (트래픽 증가 시)

### ADR-003: WebSocket vs SSE (실시간 가격)
- **결정:** WebSocket (Binance 연동용)
- **이유:**
  - Binance가 WebSocket만 지원
  - 양방향 통신 필요 (향후 주문 알림 등)
- **대안:** 클라이언트에게는 SSE로 전달 검토 가능

---

## 다음 단계

1. **Phase 1 (현재):** 기본 기능 구현
   - ✅ Spring Boot 프로젝트 생성
   - ✅ Account 도메인 완성 (Entity, Repository, Service, Controller, DTO)
   - ✅ Account 단위 테스트 완성 (22개 테스트 케이스)
   - ✅ ApiResponse 공통 응답 DTO
   - ⏳ Market 도메인 (WebSocket 가격 수집, Redis 캐싱)
   - ⏳ Order 도메인 (주문 처리 기본 로직)

2. **Phase 2:** 동시성 & 보안
   - Position 도메인
   - Redisson 분산 락
   - JWT 인증 (Auth 도메인)
   - 주문 정합성 테스트

3. **Phase 3:** 이벤트 & 확장
   - Kafka 연동
   - 비동기 처리
   - 성능 최적화

4. **Phase 4:** 운영 준비
   - 모니터링 대시보드
   - 알람 설정
   - CI/CD 파이프라인

---

## 구현 현황 (2025-01-12 기준)

### 완료된 도메인

| 도메인 | 상태 | 구현 내용 |
|--------|------|----------|
| **Account** | ✅ 완료 | Entity, Repository, Service, Controller, DTO, 테스트 |
| **Common** | ✅ 완료 | ApiResponse 공통 응답 DTO |
| **Market** | 🔄 진행중 | `PriceTick` 레코드, `LatestPriceRedisStore` (Redis 저장 로직) 구현 완료 |

### 미구현 및 예정 작업

| 도메인 | 상태 | 예정 내용 |
|--------|------|----------|
| **Market** | ⏳ 예정 | 1. `LatestPriceRedisStore` 테스트 작성<br>2. Binance WebSocket 연동 |
| **Order** | ⏳ 예정 | 주문 처리, 분산 락 |
| **Position** | ⏳ 예정 | 포지션 관리, 손익 계산 |
| **Auth** | ⏳ 예정 | JWT 인증, 로그인/회원가입 |

