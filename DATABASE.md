# DATABASE.md

## 데이터베이스 스키마 설계

실시간 암호화폐 모의투자 플랫폼의 데이터베이스 스키마 및 설계 문서입니다.

---

## 목차
1. [ERD (Entity Relationship Diagram)](#erd-entity-relationship-diagram)
2. [테이블 스키마](#테이블-스키마)
3. [인덱스 전략](#인덱스-전략)
4. [제약조건](#제약조건)
5. [마이그레이션 전략](#마이그레이션-전략)
6. [데이터 타입 선택 이유](#데이터-타입-선택-이유)
7. [샘플 데이터](#샘플-데이터)

---

## ERD (Entity Relationship Diagram)

```
┌─────────────────┐
│     users       │
│─────────────────│
│ id (PK)         │
│ username (UQ)   │
│ email (UQ)      │
│ password_hash   │
│ created_at      │
│ updated_at      │
└─────────────────┘
        │
        │ 1:1
        ↓
┌─────────────────┐
│    accounts     │
│─────────────────│
│ id (PK)         │
│ user_id (FK)    │◄──┐
│ balance         │   │
│ created_at      │   │
│ updated_at      │   │
└─────────────────┘   │
        │             │
        │ 1:N         │
        ↓             │
┌─────────────────┐   │
│     orders      │   │
│─────────────────│   │
│ id (PK)         │   │
│ account_id (FK) │───┘
│ symbol          │
│ type            │
│ price           │
│ quantity        │
│ total_amount    │
│ status          │
│ filled_at       │
│ created_at      │
└─────────────────┘
        │
        │ N:1 (symbol)
        ↓
┌─────────────────┐
│   positions     │
│─────────────────│
│ id (PK)         │
│ account_id (FK) │───┐
│ symbol          │   │
│ quantity        │   │ N:1
│ average_price   │   │
│ total_cost      │   │
│ realized_pnl    │   │
│ created_at      │   │
│ updated_at      │   │
└─────────────────┘   │
                      │
                      ↓
            ┌─────────────────┐
            │    accounts     │
            └─────────────────┘
```

---

## 테이블 스키마

### 1. users (사용자)

사용자 계정 정보를 저장합니다.

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
```

**필드 설명:**

| 컬럼 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | BIGSERIAL | NO | 사용자 ID (PK) |
| `username` | VARCHAR(50) | NO | 사용자명 (유니크, 로그인용) |
| `email` | VARCHAR(100) | NO | 이메일 (유니크) |
| `password_hash` | VARCHAR(255) | NO | BCrypt 해시된 비밀번호 |
| `created_at` | TIMESTAMP | NO | 가입 일시 |
| `updated_at` | TIMESTAMP | NO | 수정 일시 |

**JPA Entity:**
```java
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

---

### 2. accounts (계좌)

사용자의 모의투자 계좌 정보를 저장합니다.

```sql
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    balance DECIMAL(20, 8) NOT NULL DEFAULT 0.00000000,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);
```

**필드 설명:**

| 컬럼 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | BIGSERIAL | NO | 계좌 ID (PK) |
| `user_id` | BIGINT | NO | 사용자 ID (FK, 유니크 - 1:1 관계) |
| `balance` | DECIMAL(20, 8) | NO | 현금 잔고 (USDT) |
| `created_at` | TIMESTAMP | NO | 계좌 생성 일시 |
| `updated_at` | TIMESTAMP | NO | 수정 일시 |

**제약조건:**
- `balance >= 0` (체크 제약조건 - 잔고는 음수가 될 수 없음)
- `user_id` UNIQUE (한 사용자는 하나의 계좌만 보유)

**JPA Entity:**
```java
@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal balance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 도메인 메서드
    public void deductBalance(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(amount, this.balance);
        }
        this.balance = this.balance.subtract(amount);
    }

    public void addBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

---

### 3. orders (주문)

매수/매도 주문 내역을 저장합니다.

```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    type VARCHAR(10) NOT NULL,
    price DECIMAL(20, 8) NOT NULL,
    quantity DECIMAL(20, 8) NOT NULL,
    total_amount DECIMAL(20, 8) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    filled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

CREATE INDEX idx_orders_account_created ON orders(account_id, created_at DESC);
CREATE INDEX idx_orders_symbol_status ON orders(symbol, status);
CREATE INDEX idx_orders_status ON orders(status);
```

**필드 설명:**

| 컬럼 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | BIGSERIAL | NO | 주문 ID (PK) |
| `account_id` | BIGINT | NO | 계좌 ID (FK) |
| `symbol` | VARCHAR(20) | NO | 거래 페어 (예: BTCUSDT) |
| `type` | VARCHAR(10) | NO | 주문 타입 (BUY, SELL) |
| `price` | DECIMAL(20, 8) | NO | 주문 가격 |
| `quantity` | DECIMAL(20, 8) | NO | 주문 수량 |
| `total_amount` | DECIMAL(20, 8) | NO | 총 금액 (price × quantity) |
| `status` | VARCHAR(20) | NO | 주문 상태 (PENDING, FILLED, CANCELLED) |
| `filled_at` | TIMESTAMP | YES | 체결 일시 |
| `created_at` | TIMESTAMP | NO | 주문 생성 일시 |

**제약조건:**
- `type IN ('BUY', 'SELL')`
- `status IN ('PENDING', 'FILLED', 'CANCELLED')`
- `price > 0`
- `quantity > 0`

**JPA Entity:**
```java
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderType type;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(name = "total_amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "filled_at")
    private LocalDateTime filledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        // 총 금액 자동 계산
        this.totalAmount = this.price.multiply(this.quantity)
            .setScale(8, RoundingMode.HALF_UP);
    }

    // 도메인 메서드
    public void fill() {
        this.status = OrderStatus.FILLED;
        this.filledAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status == OrderStatus.FILLED) {
            throw new IllegalStateException("이미 체결된 주문은 취소할 수 없습니다.");
        }
        this.status = OrderStatus.CANCELLED;
    }
}
```

---

### 4. positions (포지션)

보유 중인 암호화폐 포지션을 저장합니다.

```sql
CREATE TABLE positions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    quantity DECIMAL(20, 8) NOT NULL,
    average_price DECIMAL(20, 8) NOT NULL,
    total_cost DECIMAL(20, 8) NOT NULL,
    realized_pnl DECIMAL(20, 8) NOT NULL DEFAULT 0.00000000,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    UNIQUE (account_id, symbol)
);

CREATE INDEX idx_positions_account_symbol ON positions(account_id, symbol);
CREATE INDEX idx_positions_account ON positions(account_id);
```

**필드 설명:**

| 컬럼 | 타입 | Null | 설명 |
|------|------|------|------|
| `id` | BIGSERIAL | NO | 포지션 ID (PK) |
| `account_id` | BIGINT | NO | 계좌 ID (FK) |
| `symbol` | VARCHAR(20) | NO | 거래 페어 (예: BTCUSDT) |
| `quantity` | DECIMAL(20, 8) | NO | 보유 수량 |
| `average_price` | DECIMAL(20, 8) | NO | 평균 매수 가격 |
| `total_cost` | DECIMAL(20, 8) | NO | 총 매수 비용 |
| `realized_pnl` | DECIMAL(20, 8) | NO | 실현 손익 (매도 시 누적) |
| `created_at` | TIMESTAMP | NO | 포지션 생성 일시 |
| `updated_at` | TIMESTAMP | NO | 수정 일시 |

**제약조건:**
- `(account_id, symbol)` UNIQUE (계좌당 심볼별 하나의 포지션만 존재)
- `quantity >= 0`

**JPA Entity:**
```java
@Entity
@Table(
    name = "positions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "symbol"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(name = "average_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal averagePrice;

    @Column(name = "total_cost", nullable = false, precision = 20, scale = 8)
    private BigDecimal totalCost;

    @Column(name = "realized_pnl", nullable = false, precision = 20, scale = 8)
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 도메인 메서드: 매수 시 평균 가격 계산
    public void addPosition(BigDecimal buyPrice, BigDecimal buyQuantity) {
        BigDecimal newTotalCost = this.totalCost.add(
            buyPrice.multiply(buyQuantity)
        );
        BigDecimal newQuantity = this.quantity.add(buyQuantity);

        this.averagePrice = newTotalCost.divide(newQuantity, 8, RoundingMode.HALF_UP);
        this.quantity = newQuantity;
        this.totalCost = newTotalCost;
    }

    // 도메인 메서드: 매도 시 수량 감소 및 실현 손익 계산
    public void reducePosition(BigDecimal sellPrice, BigDecimal sellQuantity) {
        if (this.quantity.compareTo(sellQuantity) < 0) {
            throw new InsufficientPositionException(this.symbol, sellQuantity, this.quantity);
        }

        // 실현 손익 = (매도가 - 평균 매수가) × 매도 수량
        BigDecimal pnl = sellPrice.subtract(this.averagePrice)
            .multiply(sellQuantity)
            .setScale(8, RoundingMode.HALF_UP);

        this.realizedPnl = this.realizedPnl.add(pnl);
        this.quantity = this.quantity.subtract(sellQuantity);

        // 수량 비례로 총 비용 감소
        BigDecimal costToReduce = this.averagePrice.multiply(sellQuantity);
        this.totalCost = this.totalCost.subtract(costToReduce);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

---

## 인덱스 전략

### 인덱스 선택 기준

1. **WHERE 절에 자주 사용되는 컬럼**
2. **JOIN 조건에 사용되는 FK**
3. **ORDER BY에 사용되는 컬럼**
4. **Cardinality가 높은 컬럼** (값의 다양성)

### 생성된 인덱스 목록

| 테이블 | 인덱스명 | 컬럼 | 타입 | 목적 |
|--------|---------|------|------|------|
| users | `idx_users_username` | username | B-Tree | 로그인 조회 |
| users | `idx_users_email` | email | B-Tree | 이메일 조회 |
| accounts | `idx_accounts_user_id` | user_id | B-Tree | 사용자별 계좌 조회 |
| orders | `idx_orders_account_created` | account_id, created_at DESC | B-Tree | 계좌별 주문 목록 (최신순) |
| orders | `idx_orders_symbol_status` | symbol, status | B-Tree | 심볼별 주문 필터링 |
| orders | `idx_orders_status` | status | B-Tree | 상태별 주문 조회 |
| positions | `idx_positions_account_symbol` | account_id, symbol | B-Tree | 계좌별 포지션 조회 |
| positions | `idx_positions_account` | account_id | B-Tree | 계좌별 전체 포지션 |

### 인덱스 성능 분석

```sql
-- 인덱스 사용률 확인 (PostgreSQL)
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan AS index_scans,
    idx_tup_read AS tuples_read,
    idx_tup_fetch AS tuples_fetched
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;

-- 사용되지 않는 인덱스 찾기
SELECT
    schemaname,
    tablename,
    indexname
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND indexrelname NOT LIKE '%_pkey';
```

---

## 제약조건

### Primary Keys

모든 테이블은 `BIGSERIAL` 타입의 `id` 컬럼을 PK로 사용합니다.

```sql
-- 자동 증가 ID
id BIGSERIAL PRIMARY KEY
```

### Foreign Keys

```sql
-- accounts.user_id → users.id
ALTER TABLE accounts
ADD CONSTRAINT fk_accounts_user
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- orders.account_id → accounts.id
ALTER TABLE orders
ADD CONSTRAINT fk_orders_account
FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE;

-- positions.account_id → accounts.id
ALTER TABLE positions
ADD CONSTRAINT fk_positions_account
FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE;
```

**ON DELETE CASCADE 이유:**
- 사용자 삭제 시 관련 계좌, 주문, 포지션 모두 삭제
- 데이터 정합성 보장
- 고아 레코드 방지

### Unique Constraints

```sql
-- users 테이블
ALTER TABLE users ADD CONSTRAINT uq_users_username UNIQUE (username);
ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);

-- accounts 테이블
ALTER TABLE accounts ADD CONSTRAINT uq_accounts_user UNIQUE (user_id);

-- positions 테이블
ALTER TABLE positions ADD CONSTRAINT uq_positions_account_symbol UNIQUE (account_id, symbol);
```

### Check Constraints

```sql
-- accounts 테이블
ALTER TABLE accounts
ADD CONSTRAINT chk_accounts_balance_positive
CHECK (balance >= 0);

-- orders 테이블
ALTER TABLE orders
ADD CONSTRAINT chk_orders_type
CHECK (type IN ('BUY', 'SELL'));

ALTER TABLE orders
ADD CONSTRAINT chk_orders_status
CHECK (status IN ('PENDING', 'FILLED', 'CANCELLED'));

ALTER TABLE orders
ADD CONSTRAINT chk_orders_price_positive
CHECK (price > 0);

ALTER TABLE orders
ADD CONSTRAINT chk_orders_quantity_positive
CHECK (quantity > 0);

-- positions 테이블
ALTER TABLE positions
ADD CONSTRAINT chk_positions_quantity_non_negative
CHECK (quantity >= 0);
```

---

## 마이그레이션 전략

### Flyway 마이그레이션

프로젝트는 Flyway를 사용하여 데이터베이스 마이그레이션을 관리합니다.

**의존성 추가:**
```gradle
implementation 'org.flywaydb:flyway-core'
runtimeOnly 'org.flywaydb:flyway-database-postgresql'
```

**설정:**
```yaml
# application.yml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
```

### 마이그레이션 파일 구조

```
src/main/resources/db/migration/
├── V1__create_users_table.sql
├── V2__create_accounts_table.sql
├── V3__create_orders_table.sql
├── V4__create_positions_table.sql
├── V5__add_indexes.sql
└── V6__add_constraints.sql
```

### V1__create_users_table.sql

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
```

### V2__create_accounts_table.sql

```sql
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    balance DECIMAL(20, 8) NOT NULL DEFAULT 0.00000000,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_accounts_balance_positive CHECK (balance >= 0)
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);
```

### V3__create_orders_table.sql

```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    type VARCHAR(10) NOT NULL,
    price DECIMAL(20, 8) NOT NULL,
    quantity DECIMAL(20, 8) NOT NULL,
    total_amount DECIMAL(20, 8) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    filled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT chk_orders_type CHECK (type IN ('BUY', 'SELL')),
    CONSTRAINT chk_orders_status CHECK (status IN ('PENDING', 'FILLED', 'CANCELLED')),
    CONSTRAINT chk_orders_price_positive CHECK (price > 0),
    CONSTRAINT chk_orders_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_orders_account_created ON orders(account_id, created_at DESC);
CREATE INDEX idx_orders_symbol_status ON orders(symbol, status);
CREATE INDEX idx_orders_status ON orders(status);
```

### V4__create_positions_table.sql

```sql
CREATE TABLE positions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    quantity DECIMAL(20, 8) NOT NULL,
    average_price DECIMAL(20, 8) NOT NULL,
    total_cost DECIMAL(20, 8) NOT NULL,
    realized_pnl DECIMAL(20, 8) NOT NULL DEFAULT 0.00000000,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_positions_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT uq_positions_account_symbol UNIQUE (account_id, symbol),
    CONSTRAINT chk_positions_quantity_non_negative CHECK (quantity >= 0)
);

CREATE INDEX idx_positions_account_symbol ON positions(account_id, symbol);
CREATE INDEX idx_positions_account ON positions(account_id);
```

---

## 데이터 타입 선택 이유

### BIGSERIAL vs INT

**선택: BIGSERIAL**

- 최대 값: 9,223,372,036,854,775,807 (약 922경)
- INT의 최대값 (21억)보다 훨씬 큼
- 향후 데이터 증가에 대비

### DECIMAL(20, 8)

**금융 데이터에 DECIMAL 사용 이유:**

- **정확성:** 부동소수점 오류 없음
- **정밀도:** 소수점 8자리 (암호화폐 표준)
- **범위:** 최대 999,999,999,999.99999999

**잘못된 타입 (절대 사용 금지):**
```sql
-- ❌ FLOAT, DOUBLE - 부동소수점 오류 발생
price FLOAT  -- 0.1 + 0.2 = 0.30000000000000004

-- ✅ DECIMAL - 정확한 값
price DECIMAL(20, 8)  -- 0.1 + 0.2 = 0.3
```

### VARCHAR vs TEXT

**선택 기준:**

- **VARCHAR(N):** 길이 제한이 명확한 경우 (username, email, symbol)
- **TEXT:** 길이 제한이 없는 경우 (설명, 메모 등 - 현재 프로젝트에서는 미사용)

### TIMESTAMP vs TIMESTAMPTZ

**선택: TIMESTAMP**

- 현재는 서버 로컬 시간 사용
- 향후 글로벌 서비스 시 TIMESTAMPTZ (시간대 포함) 고려

---

## 샘플 데이터

### 초기 데이터 (테스트용)

```sql
-- 테스트 사용자
INSERT INTO users (username, email, password_hash)
VALUES ('testuser', 'test@example.com', '$2a$12$hash...'); -- BCrypt 해시

-- 계좌 생성 (초기 잔고 10,000 USDT)
INSERT INTO accounts (user_id, balance)
VALUES (1, 10000.00000000);

-- 매수 주문
INSERT INTO orders (account_id, symbol, type, price, quantity, total_amount, status, filled_at)
VALUES (1, 'BTCUSDT', 'BUY', 45000.00000000, 0.10000000, 4500.00000000, 'FILLED', NOW());

-- 포지션 생성
INSERT INTO positions (account_id, symbol, quantity, average_price, total_cost)
VALUES (1, 'BTCUSDT', 0.10000000, 45000.00000000, 4500.00000000);
```

### 데이터 조회 쿼리

```sql
-- 사용자별 총 자산 조회
SELECT
    u.username,
    a.balance AS cash,
    COALESCE(SUM(p.quantity * p.average_price), 0) AS position_value,
    a.balance + COALESCE(SUM(p.quantity * p.average_price), 0) AS total_asset
FROM users u
JOIN accounts a ON u.id = a.user_id
LEFT JOIN positions p ON a.id = p.account_id
WHERE u.id = 1
GROUP BY u.username, a.balance;

-- 주문 내역 조회 (최근 10건)
SELECT
    o.id,
    o.symbol,
    o.type,
    o.price,
    o.quantity,
    o.total_amount,
    o.status,
    o.created_at
FROM orders o
WHERE o.account_id = 1
ORDER BY o.created_at DESC
LIMIT 10;

-- 포지션 현황 (미실현 손익 포함)
SELECT
    p.symbol,
    p.quantity,
    p.average_price,
    45500.00000000 AS current_price, -- Redis에서 조회한 현재가
    (45500.00000000 - p.average_price) * p.quantity AS unrealized_pnl
FROM positions p
WHERE p.account_id = 1 AND p.quantity > 0;
```

---

## 성능 튜닝

### 1. 비관적 락 (Pessimistic Lock)

동시 주문 시 데이터 정합성을 보장하기 위해 비관적 락 사용:

```java
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);
}
```

**SQL:**
```sql
SELECT * FROM accounts WHERE id = 1 FOR UPDATE;
```

### 2. 쿼리 최적화

**N+1 문제 해결:**
```java
@Query("SELECT o FROM Order o JOIN FETCH o.account WHERE o.accountId = :accountId")
List<Order> findByAccountIdWithAccount(@Param("accountId") Long accountId);
```

**Projection 사용:**
```java
@Query("SELECT new OrderSummaryDto(o.id, o.symbol, o.price, o.status) " +
       "FROM Order o WHERE o.accountId = :accountId")
List<OrderSummaryDto> findOrderSummaries(@Param("accountId") Long accountId);
```

### 3. 파티셔닝 (향후 고려)

orders 테이블이 커지면 날짜별 파티셔닝 적용:

```sql
-- 월별 파티셔닝
CREATE TABLE orders_2025_01 PARTITION OF orders
FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE orders_2025_02 PARTITION OF orders
FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');
```

---

## 백업 전략

### 1. 자동 백업 (PostgreSQL)

```bash
# 매일 자정 전체 백업
0 0 * * * pg_dump -U postgres trade_db > /backups/trade_db_$(date +\%Y\%m\%d).sql
```

### 2. Point-in-Time Recovery (PITR)

```sql
-- WAL (Write-Ahead Logging) 활성화
ALTER SYSTEM SET wal_level = replica;
ALTER SYSTEM SET archive_mode = on;
ALTER SYSTEM SET archive_command = 'cp %p /archive/%f';
```

### 3. 복구 절차

```bash
# 백업 파일에서 복구
psql -U postgres -d trade_db < /backups/trade_db_20251226.sql
```

---

## 다음 단계

1. **Flyway 마이그레이션 파일 작성**
2. **JPA Entity 생성 및 Repository 구현**
3. **테스트 데이터 삽입 스크립트 작성**
4. **성능 테스트 및 인덱스 튜닝**
5. **운영 DB (PostgreSQL) 환경 구축**

---

## 참고 링크

- [CLAUDE.md](./CLAUDE.md) - 프로젝트 개요 및 코딩 가이드
- [ARCHITECTURE.md](./ARCHITECTURE.md) - 시스템 아키텍처 상세
- [API.md](./API.md) - REST API 명세서
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [PostgreSQL DECIMAL 문서](https://www.postgresql.org/docs/current/datatype-numeric.html)
