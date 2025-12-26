# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 비전

**실시간 암호화폐 모의투자 플랫폼**

Binance WebSocket으로 실시간 가격을 받아 서버에 캐시하고, 그 가격으로 모의 매수·매도 주문을 처리하는 투자 플랫폼입니다.

Redis 락으로 동시 주문 정합성을 보장하면서, 주문이 성공하면 MQ로 주문 이벤트를 발행해 핵심 로직과 부가 작업을 분리합니다.

JWT 인증 + JPA 트랜잭션 도메인(Account·Position·Order) 위에서 설계한 **실시간·동시성·이벤트 드리븐 백엔드 시스템**입니다.

**목적:** 이직 포트폴리오 - 실무 수준의 백엔드 역량 증명

---

## 핵심 아키텍처 설계

### 3대 핵심 요구사항
1. **실시간 처리** - Binance WebSocket → Redis 캐시 → 즉시 주문 처리
2. **동시성 제어** - Redis 분산 락으로 동시 주문의 데이터 정합성 보장
3. **이벤트 드리븐** - MQ로 주문 이벤트 발행, 핵심 로직과 부가 작업 분리

### 데이터 흐름

```
[Binance WebSocket]
       ↓ (실시간 가격)
[Redis Cache] ← 최신 가격 TTL 관리
       ↓
[주문 API] ← JWT 인증된 사용자
       ↓
[Redis 분산 락] ← 동시성 제어
       ↓
[주문 처리 Service] ← 계좌 잔고 확인, 포지션 생성/수정
       ↓
[JPA Transaction] ← Account, Position, Order 저장
       ↓
[MQ 이벤트 발행] ← 주문 완료 이벤트
       ↓
[비동기 후처리] ← 알림, 로그, 통계 등
```

---

## 기술 스택

- **Framework:** Spring Boot 4.0.0
- **Language:** Java 17
- **Build:** Gradle
- **Database:** H2 (개발), PostgreSQL/MySQL (운영 예정)
- **Cache:** Redis (가격 캐시 + 분산 락)
- **WebSocket:** Spring WebSocket Client (Binance 연동)
- **ORM:** Spring Data JPA
- **MQ:** RabbitMQ 또는 Kafka (주문 이벤트)
- **Security:** Spring Security + JWT 인증
- **Testing:** JUnit 5, Mockito, TestContainers

---

## 도메인 모델 설계

### Account (계좌)
- 사용자의 모의투자 계좌
- 잔고(balance) 관리
- 초기 자금 제공

### Position (포지션)
- 보유 중인 암호화폐 수량
- 평균 매수가 계산
- 실현/미실현 손익 추적

### Order (주문)
- 매수/매도 주문 기록
- 주문 상태: PENDING, FILLED, CANCELLED
- 체결 가격, 수량, 타임스탬프

### PriceTick (가격 틱)
- Binance에서 받은 실시간 가격
- Redis 캐시에만 저장 (휘발성)

---

## 패키지 구조 (DDD 레이어드 아키텍처)

```
trade.tradestream/
├── common/              # 공통 유틸리티, DTO
├── config/              # 전역 설정 (Redis, Security, MQ 등)
├── auth/                # 인증/인가 (JWT)
│   ├── api/             # REST Controller
│   ├── application/     # Service (비즈니스 로직)
│   ├── domain/          # Entity, VO, 도메인 로직
│   └── infra/           # Repository, 외부 연동
├── account/             # 계좌 도메인
│   ├── api/
│   ├── application/
│   ├── domain/
│   └── infra/
├── market/              # 시장 가격
│   ├── api/
│   ├── application/     # WebSocket 클라이언트 포함
│   ├── domain/
│   └── infra/
├── order/               # 주문 도메인
│   ├── api/
│   ├── application/
│   ├── domain/
│   └── infra/
└── position/            # 포지션 도메인
    ├── api/
    ├── application/
    ├── domain/
    └── infra/
```

**계층별 의존성 규칙:**
- api → application → domain
- infra는 독립적으로 구현, application에서 주입받음
- 도메인은 외부 의존성 없음 (순수 비즈니스 로직)

---

## 코딩 스타일 및 원칙

### 핵심 원칙

1. **기능 변경 금지 (Behavior must remain identical)**
   - 리팩토링 시 동작은 절대 변경하지 않음
   - 테스트로 동작 일치 보장

2. **불변 데이터 선호**
   - Java Record 적극 활용 (DTO, VO)
   - 필드는 `final` 사용
   - Setter 사용 금지, Builder 패턴 사용

3. **단일 책임 원칙(SRP) 우선 적용**
   - 한 클래스는 한 가지 이유로만 변경되어야 함
   - 메서드는 한 가지 일만 수행

4. **결합도↓ 응집도↑**
   - 인터페이스를 통한 의존성 주입
   - 관련 있는 로직은 같은 클래스에 모음
   - 관련 없는 로직은 분리

5. **명확한 모듈 경계 유지**
   - 도메인 간 직접 참조 금지
   - 공통 로직은 common 패키지로
   - 순환 참조 절대 금지

6. **"좋은 추상화가 나쁜 조건문을 이긴다"**
   - 복잡한 if/else 대신 전략 패턴, 다형성 활용
   - enum으로 상태 관리
   - null 대신 Optional 사용

7. **테스트가 없다면 → 가벼운 테스트 추가 제안**
   - 모든 비즈니스 로직은 테스트 필수
   - Given-When-Then 패턴 사용

### 리팩토링 기법

**메서드 추출 (Extract Method)**
```java
// Before
public void processOrder() {
    // 검증 로직 10줄
    // 계산 로직 15줄
    // 저장 로직 5줄
}

// After
public void processOrder() {
    validate();
    calculate();
    save();
}
```

**변수 이름 변경 (Rename Variable)**
```java
// Before
int a = account.getBalance();

// After
int accountBalance = account.getBalance();
```

**클래스 추출 (Extract Class)**
```java
// Before: OrderService가 주문 처리 + 가격 계산 + 알림 전송

// After:
// OrderService - 주문 처리만
// PriceCalculator - 가격 계산만
// NotificationSender - 알림 전송만
```

**메서드 이동 (Move Method)**
```java
// Before: OrderService에서 Position 계산

// After: PositionService로 이동
```

**조건문 단순화 (Simplify Conditionals)**
```java
// Before
if (order.getType() == OrderType.BUY && order.getStatus() == OrderStatus.PENDING) {
    // ...
}

// After
if (order.isPendingBuyOrder()) {
    // ...
}
```

**루프 단순화 (Simplify Loops)**
```java
// Before
for (int i = 0; i < orders.size(); i++) {
    Order order = orders.get(i);
    if (order.isFilled()) {
        sum += order.getPrice();
    }
}

// After
BigDecimal sum = orders.stream()
    .filter(Order::isFilled)
    .map(Order::getPrice)
    .reduce(BigDecimal.ZERO, BigDecimal::add);
```

---

## Java 코딩 컨벤션

### 기본 스타일
- **들여쓰기:** 4칸 (스페이스)
- **네이밍:**
  - camelCase: 변수, 메서드
  - PascalCase: 클래스, 인터페이스
  - UPPER_SNAKE_CASE: 상수
- **Lombok:** `@RequiredArgsConstructor`, `@Getter`, `@Builder` 사용
- **Record:** 불변 DTO는 Java Record 사용

### 금융 계산 규칙 (매우 중요)
```java
// ❌ 절대 사용 금지
double price = 1.1;
float amount = 0.1f;

// ✅ 반드시 사용
BigDecimal price = new BigDecimal("1.1");
BigDecimal amount = BigDecimal.valueOf(0.1);

// 연산 시 반올림 명시
BigDecimal result = price.multiply(amount)
    .setScale(8, RoundingMode.HALF_UP);
```

- **절대 float/double 사용 금지** → 모든 금액, 가격은 `BigDecimal`
- **반올림:** `RoundingMode.HALF_UP` (기본)
- **정밀도:** 소수점 8자리 (암호화폐 표준)

### 불변 객체 패턴
```java
// Record 사용 (권장)
public record PriceRequest(String symbol, BigDecimal price) {}

// 또는 Builder 패턴
@Builder
public class Order {
    private final Long id;
    private final String symbol;
    private final BigDecimal price;

    // Getter만 제공, Setter 없음
}
```

### Optional 사용
```java
// ❌ null 체크
if (account == null) {
    throw new AccountNotFoundException();
}

// ✅ Optional 사용
Optional<Account> accountOpt = accountRepository.findById(id);
Account account = accountOpt.orElseThrow(() ->
    new AccountNotFoundException(id));
```

### 예외 처리
```java
// 비즈니스 예외는 명확한 이름 사용
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(BigDecimal required, BigDecimal actual) {
        super(String.format("잔고 부족: 필요=%s, 보유=%s", required, actual));
    }
}
```

---

## 트랜잭션 & 동시성 제어

### 트랜잭션 관리
```java
// Service 레이어에서만 @Transactional 사용
@Service
@RequiredArgsConstructor
public class OrderService {

    // 쓰기 작업
    @Transactional
    public Order createOrder(OrderRequest request) {
        // ...
    }

    // 읽기 전용
    @Transactional(readOnly = true)
    public Order getOrder(Long orderId) {
        // ...
    }
}
```

### Redis 분산 락 패턴
```java
@Component
@RequiredArgsConstructor
public class OrderService {
    private final RedissonClient redissonClient;

    public Order createOrder(Long accountId, OrderRequest request) {
        String lockKey = "order:lock:" + accountId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 3초 대기, 10초 후 자동 해제
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                return processOrder(accountId, request);
            } else {
                throw new ConcurrentOrderException("다른 주문 처리 중");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrderProcessingException("주문 처리 중단", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### 이벤트 발행 패턴
```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order executeOrder(OrderRequest request) {
        // 1. 주문 처리
        Order order = processOrder(request);

        // 2. DB 저장
        orderRepository.save(order);

        // 3. 이벤트 발행 (트랜잭션 커밋 후)
        eventPublisher.publishEvent(new OrderFilledEvent(order));

        return order;
    }
}

// 이벤트 리스너 (비동기 처리)
@Component
public class OrderEventListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderFilled(OrderFilledEvent event) {
        // 알림, 로그, 통계 등 부가 작업
    }
}
```

---

## API 설계 원칙

### REST API 컨벤션
- **POST /api/auth/login** - 로그인 (JWT 발급)
- **POST /api/auth/register** - 회원가입
- **GET /api/accounts/{id}** - 계좌 조회
- **POST /api/orders** - 주문 생성
- **GET /api/orders/{id}** - 주문 조회
- **GET /api/orders** - 주문 목록
- **GET /api/positions** - 보유 포지션 목록
- **GET /api/market/prices/{symbol}** - 현재 가격 조회

### 표준 응답 형식
```java
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, "성공", LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, LocalDateTime.now());
    }
}
```

---

## 테스트 전략

### 단위 테스트 (Service 레이어)
```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("주문 생성 시 잔고가 충분하면 성공한다")
    void createOrder_Success() {
        // Given
        OrderRequest request = new OrderRequest("BTCUSDT", BigDecimal.valueOf(100));
        Account account = Account.builder()
            .balance(BigDecimal.valueOf(1000))
            .build();

        // When
        Order result = orderService.createOrder(account, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.FILLED);
    }
}
```

### 통합 테스트
```java
@SpringBootTest
@Testcontainers
class OrderIntegrationTest {
    @Container
    static RedisContainer redis = new RedisContainer();

    @Autowired
    private OrderService orderService;

    @Test
    void 동시_주문_시_정합성_검증() throws Exception {
        // 멀티 스레드 동시성 테스트
    }
}
```

---

## 빌드 & 실행 명령어

```bash
# 프로젝트 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 단일 테스트 클래스 실행
./gradlew test --tests OrderServiceTest

# 애플리케이션 실행
./gradlew bootRun

# 클린 빌드
./gradlew clean build
```

---

## 중요 주의사항

### 금융 데이터 정확성
- **절대 float/double 사용 금지** → BigDecimal만 사용
- **반올림 오류 주의** → 명시적 RoundingMode 지정
- **트랜잭션 격리 수준** → READ_COMMITTED 이상

### 동시성 이슈
- **Redis 락 타임아웃** → 적절한 대기/해제 시간 설정
- **락 해제 보장** → finally 블록에서 unlock
- **데드락 방지** → 락 순서 일관성 유지

### 보안
- **JWT Secret** → 환경 변수로 관리 (.env 파일, 절대 커밋 금지)
- **비밀번호** → BCrypt 해싱
- **민감 정보** → 로그에 출력 금지

### 성능 최적화
- **Redis 캐시 활용** → 자주 조회되는 데이터
- **DB 인덱스** → symbol, accountId, userId
- **N+1 문제 방지** → fetch join 사용
- **페이징** → 대량 데이터 조회 시 필수

---

## 면접 대비 핵심 포인트

이 프로젝트로 증명할 수 있는 역량:

1. **실시간 데이터 처리** - WebSocket, Redis 캐싱
2. **동시성 제어** - 분산 락, 트랜잭션 격리
3. **이벤트 드리븐 아키텍처** - MQ, 비동기 처리
4. **도메인 주도 설계** - 명확한 계층 분리, 도메인 로직
5. **금융 시스템 정확성** - BigDecimal, 정합성 검증
6. **리팩토링 역량** - 클린 코드, SOLID 원칙
7. **테스트 전략** - 단위/통합/동시성 테스트
8. **확장 가능한 설계** - 모듈화, 의존성 역전

---

## 추가 문서

이 프로젝트의 상세 정보는 다음 문서에서 확인할 수 있습니다:

- **[ARCHITECTURE.md](./ARCHITECTURE.md)** - 시스템 아키텍처 상세 설계
  - 전체 시스템 구조 및 컴포넌트 간 통신
  - Binance WebSocket 연동 상세 구현
  - Redis 캐싱 전략 및 분산 락 설계
  - 이벤트 드리븐 아키텍처 (Spring Events, Kafka)
  - JWT 인증 흐름 및 보안 설정
  - 성능 최적화 전략 및 확장성 고려사항

- **[API.md](./API.md)** - REST API 명세서
  - 모든 엔드포인트 상세 스펙 (인증, 계좌, 주문, 포지션, 시장 가격)
  - 요청/응답 예시 및 에러 코드
  - Rate Limiting 정책
  - 페이지네이션 및 필터링
  - Postman Collection 사용법

- **[DATABASE.md](./DATABASE.md)** - 데이터베이스 스키마 및 설계
  - ERD (Entity Relationship Diagram)
  - 테이블 스키마 및 JPA Entity 예시
  - 인덱스 전략 및 성능 최적화
  - 제약조건 및 데이터 타입 선택 이유
  - Flyway 마이그레이션 파일
  - 백업 전략 및 복구 절차

---

## 참고 자료

- Binance WebSocket API: https://binance-docs.github.io/apidocs/spot/en/#websocket-market-streams
- Redis 분산 락: https://redis.io/docs/manual/patterns/distributed-locks/
- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- Redisson: https://github.com/redisson/redisson
- Clean Code (Robert C. Martin)
- Refactoring (Martin Fowler)
