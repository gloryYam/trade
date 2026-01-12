# API.md

## REST API 명세서

실시간 암호화폐 모의투자 플랫폼의 API 엔드포인트 상세 명세입니다.

---

## 목차
1. [API 개요](#api-개요)
2. [인증 (Auth)](#인증-auth)
3. [계좌 (Account)](#계좌-account)
4. [주문 (Order)](#주문-order)
5. [포지션 (Position)](#포지션-position)
6. [시장 가격 (Market)](#시장-가격-market)
7. [에러 코드](#에러-코드)

---

## API 개요

### Base URL
```
개발: http://localhost:8080
운영: https://api.trade-platform.com
```

### 공통 헤더

**요청 헤더:**
```http
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}
```

**응답 헤더:**
```http
Content-Type: application/json
X-Request-ID: {UUID}
X-RateLimit-Remaining: 100
```

### 표준 응답 형식

**성공 응답:**
```json
{
  "success": true,
  "data": {
    // 실제 데이터
  },
  "message": "성공",
  "timestamp": "2025-12-26T10:30:00Z"
}
```

**실패 응답:**
```json
{
  "success": false,
  "data": null,
  "message": "에러 메시지",
  "timestamp": "2025-12-26T10:30:00Z",
  "errorCode": "INSUFFICIENT_BALANCE",
  "details": {
    "required": "100.00",
    "available": "50.00"
  }
}
```

### HTTP 상태 코드

| 상태 코드 | 설명 |
|---------|------|
| 200 OK | 요청 성공 |
| 201 Created | 리소스 생성 성공 |
| 400 Bad Request | 잘못된 요청 파라미터 |
| 401 Unauthorized | 인증 실패 (토큰 없음/만료) |
| 403 Forbidden | 권한 없음 |
| 404 Not Found | 리소스 없음 |
| 409 Conflict | 중복 요청, 동시성 충돌 |
| 429 Too Many Requests | Rate Limit 초과 |
| 500 Internal Server Error | 서버 내부 오류 |

---

## 인증 (Auth)

### 1. 회원가입

**POST** `/api/auth/register`

사용자 계정을 생성합니다.

**요청:**
```json
{
  "username": "user123",
  "email": "user@example.com",
  "password": "securePassword123!",
  "confirmPassword": "securePassword123!"
}
```

**응답: 201 Created**
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "username": "user123",
    "email": "user@example.com",
    "createdAt": "2025-12-26T10:30:00Z"
  },
  "message": "회원가입 성공",
  "timestamp": "2025-12-26T10:30:00Z"
}
```

**에러:**
- `400` - 비밀번호 불일치, 유효성 검증 실패
- `409` - 이미 존재하는 username/email

---

### 2. 로그인

**POST** `/api/auth/login`

JWT 토큰을 발급받습니다.

**요청:**
```json
{
  "username": "user123",
  "password": "securePassword123!"
}
```

**응답: 200 OK**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "userId": 1,
      "username": "user123",
      "email": "user@example.com"
    }
  },
  "message": "로그인 성공",
  "timestamp": "2025-12-26T10:30:00Z"
}
```

**에러:**
- `401` - 잘못된 username/password
- `403` - 계정 잠김 (5회 이상 로그인 실패)

---

### 3. 로그아웃

**POST** `/api/auth/logout`

현재 토큰을 무효화합니다.

**요청 헤더:**
```http
Authorization: Bearer {JWT_TOKEN}
```

**응답: 200 OK**
```json
{
  "success": true,
  "data": null,
  "message": "로그아웃 성공",
  "timestamp": "2025-12-26T10:30:00Z"
}
```

---

### 4. 토큰 갱신

**POST** `/api/auth/refresh`

만료된 토큰을 갱신합니다.

**요청:**
```json
{
  "refreshToken": "refresh_token_string"
}
```

**응답: 200 OK**
```json
{
  "success": true,
  "data": {
    "accessToken": "new_jwt_token",
    "tokenType": "Bearer",
    "expiresIn": 3600
  },
  "message": "토큰 갱신 성공",
  "timestamp": "2025-12-26T10:30:00Z"
}
```

---

## 계좌 (Account)

> **구현 상태:** ✅ 완료 (2025-01-12)

### 1. 계좌 생성 ✅

**POST** `/api/accounts`

새 계좌를 생성합니다.

**요청:**
```json
{
  "userId": 1,
  "initialBalance": "10000.00"
}
```

**필드 검증:**
- `userId`: 필수, 양수
- `initialBalance`: 필수, 양수

**응답: 200 OK**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 1,
    "balance": "10000.00"
  },
  "message": "성공",
  "timestamp": "2025-01-12T10:30:00"
}
```

**에러:**
- `400` - 유효성 검증 실패 (음수 userId, 음수 잔고 등)

---

### 2. 계좌 ID로 조회 ✅

**GET** `/api/accounts/{id}`

특정 계좌 정보를 조회합니다.

**응답: 200 OK**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 1,
    "balance": "10000.00"
  },
  "message": "성공",
  "timestamp": "2025-01-12T10:30:00"
}
```

**에러:**
- `404` - 존재하지 않는 계좌 (AccountNotFoundException)

---

### 3. 사용자 ID로 계좌 조회 ✅

**GET** `/api/accounts/user/{userId}`

사용자 ID로 계좌를 조회합니다.

**응답: 200 OK**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 1,
    "balance": "10000.00"
  },
  "message": "성공",
  "timestamp": "2025-01-12T10:30:00"
}
```

**에러:**
- `404` - 해당 사용자의 계좌가 존재하지 않음

---

### 4. 내 계좌 조회 (예정)

**GET** `/api/accounts/me`

> ⏳ JWT 인증 구현 후 추가 예정

현재 로그인한 사용자의 계좌 정보를 조회합니다.

---

### 5. 입금 (예정)

**POST** `/api/accounts/deposit`

> ⏳ 추후 구현 예정

모의투자 계좌에 자금을 입금합니다.

---

### 6. 출금 (예정)

**POST** `/api/accounts/withdraw`

> ⏳ 추후 구현 예정

계좌에서 자금을 출금합니다.

---

## 주문 (Order)

### 1. 주문 생성

**POST** `/api/orders`

매수 또는 매도 주문을 생성합니다.

**요청:**
```json
{
  "symbol": "BTCUSDT",
  "type": "BUY",
  "price": "45000.00000000",
  "quantity": "0.10000000"
}
```

**필드 설명:**
- `symbol`: 거래 페어 (예: BTCUSDT, ETHUSDT)
- `type`: 주문 타입 (`BUY` 또는 `SELL`)
- `price`: 주문 가격 (현재는 시장가로 즉시 체결)
- `quantity`: 주문 수량

**응답: 201 Created**
```json
{
  "success": true,
  "data": {
    "orderId": 1001,
    "accountId": 1,
    "symbol": "BTCUSDT",
    "type": "BUY",
    "price": "45000.00000000",
    "quantity": "0.10000000",
    "totalAmount": "4500.00000000",
    "status": "FILLED",
    "filledAt": "2025-12-26T10:30:00Z",
    "createdAt": "2025-12-26T10:30:00Z"
  },
  "message": "주문 체결 완료",
  "timestamp": "2025-12-26T10:30:00Z"
}
```

**에러:**
- `400` - 잘못된 파라미터 (음수 가격/수량, 지원하지 않는 심볼)
- `409` - 잔고 부족 (매수 시), 보유 수량 부족 (매도 시)
- `409` - 동시 주문 처리 중 (분산 락 획득 실패)

---

### 2. 주문 조회

**GET** `/api/orders/{orderId}`

특정 주문의 상세 정보를 조회합니다.

**응답: 200 OK**
```json
{
  "success": true,
  "data": {
    "orderId": 1001,
    "accountId": 1,
    "symbol": "BTCUSDT",
    "type": "BUY",
    "price": "45000.00000000",
    "quantity": "0.10000000",
    "totalAmount": "4500.00000000",
    "status": "FILLED",
    "filledAt": "2025-12-26T10:30:00Z",
    "createdAt": "2025-12-26T10:30:00Z"
  },
  "message": "성공",
  "timestamp": "2025-12-26T10:30:00Z"
}
```

**에러:**
- `403` - 다른 사용자의 주문 조회 시도
- `404` - 존재하지 않는 주문

---

### 3. 주문 목록 조회

**GET** `/api/orders`

현재 사용자의 주문 목록을 조회합니다.

**쿼리 파라미터:**
- `symbol` (optional): 필터링할 심볼 (예: BTCUSDT)
- `status` (optional): 필터링할 상태 (PENDING, FILLED, CANCELLED)
- `page` (optional, default=0): 페이지 번호
- `size` (optional, default=20): 페이지 크기
- `sort` (optional, default=createdAt,desc): 정렬 기준

**요청 예시:**
```
GET /api/orders?symbol=BTCUSDT&status=FILLED&page=0&size=10
```

**응답: 200 OK**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "orderId": 1001,
        "symbol": "BTCUSDT",
        "type": "BUY",
        "price": "45000.00000000",
        "quantity": "0.10000000",
        "status": "FILLED",
        "createdAt": "2025-12-26T10:30:00Z"
      },
      {
        "orderId": 1002,
        "symbol": "BTCUSDT",
        "type": "SELL",
        "price": "46000.00000000",
        "quantity": "0.05000000",
        "status": "FILLED",
        "createdAt": "2025-12-26T11:00:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 10,
      "totalElements": 25,
      "totalPages": 3
    }
  },
  "message": "성공",
  "timestamp": "2025-12-26T10:30:00Z"
}
```

---

### 4. 주문 취소

**DELETE** `/api/orders/{orderId}`

대기 중인 주문을 취소합니다. (현재는 즉시 체결이므로 사용하지 않음)

**응답: 200 OK**
```json
{
  "success": true,
  "data": {
    "orderId": 1001,
    "status": "CANCELLED",
    "cancelledAt": "2025-12-26T10:31:00Z"
  },
  "message": "주문 취소 완료",
  "timestamp": "2025-12-26T10:31:00Z"
}
```

**에러:**
- `400` - 이미 체결된 주문은 취소 불가
- `404` - 존재하지 않는 주문

---

## 포지션 (Position)

### 1. 포지션 목록 조회

**GET** `/api/positions`

현재 보유 중인 포지션 목록을 조회합니다.

**응답: 200 OK**
```json
{
  "success": true,
  "data": [
    {
      "positionId": 1,
      "accountId": 1,
      "symbol": "BTCUSDT",
      "quantity": "0.10000000",
      "averagePrice": "45000.00000000",
      "currentPrice": "46000.00000000",
      "totalCost": "4500.00000000",
      "currentValue": "4600.00000000",
      "unrealizedPnL": "+100.00000000",
      "unrealizedPnLPercent": "+2.22",
      "createdAt": "2025-12-26T10:30:00Z",
      "updatedAt": "2025-12-26T12:00:00Z"
    },
    {
      "positionId": 2,
      "accountId": 1,
      "symbol": "ETHUSDT",
      "quantity": "1.50000000",
      "averagePrice": "2400.00000000",
      "currentPrice": "2380.00000000",
      "totalCost": "3600.00000000",
      "currentValue": "3570.00000000",
      "unrealizedPnL": "-30.00000000",
      "unrealizedPnLPercent": "-0.83",
      "createdAt": "2025-12-26T11:00:00Z",
      "updatedAt": "2025-12-26T12:00:00Z"
    }
  ],
  "message": "성공",
  "timestamp": "2025-12-26T12:00:00Z"
}
```

**필드 설명:**
- `quantity`: 보유 수량
- `averagePrice`: 평균 매수 가격
- `currentPrice`: 현재 시장 가격 (Redis 캐시)
- `totalCost`: 총 매수 비용
- `currentValue`: 현재 평가 가치
- `unrealizedPnL`: 미실현 손익 (currentValue - totalCost)
- `unrealizedPnLPercent`: 미실현 수익률 (%)

---

### 2. 포지션 상세 조회

**GET** `/api/positions/{positionId}`

특정 포지션의 상세 정보를 조회합니다.

**응답: 200 OK**
```json
{
  "success": true,
  "data": {
    "positionId": 1,
    "accountId": 1,
    "symbol": "BTCUSDT",
    "quantity": "0.10000000",
    "averagePrice": "45000.00000000",
    "currentPrice": "46000.00000000",
    "unrealizedPnL": "+100.00000000",
    "trades": [
      {
        "orderId": 1001,
        "type": "BUY",
        "price": "45000.00000000",
        "quantity": "0.10000000",
        "executedAt": "2025-12-26T10:30:00Z"
      }
    ]
  },
  "message": "성공",
  "timestamp": "2025-12-26T12:00:00Z"
}
```

**에러:**
- `403` - 다른 사용자의 포지션 조회 시도
- `404` - 존재하지 않는 포지션

---

### 3. 심볼별 포지션 조회

**GET** `/api/positions/by-symbol/{symbol}`

특정 심볼의 포지션만 조회합니다.

**요청 예시:**
```
GET /api/positions/by-symbol/BTCUSDT
```

**응답: 200 OK**
```json
{
  "success": true,
  "data": {
    "positionId": 1,
    "symbol": "BTCUSDT",
    "quantity": "0.10000000",
    "averagePrice": "45000.00000000",
    "currentPrice": "46000.00000000",
    "unrealizedPnL": "+100.00000000"
  },
  "message": "성공",
  "timestamp": "2025-12-26T12:00:00Z"
}
```

**에러:**
- `404` - 해당 심볼의 포지션 없음

---

## 시장 가격 (Market)

### 1. 현재 가격 조회

**GET** `/api/market/prices/{symbol}`

특정 심볼의 최신 가격을 조회합니다. (Redis 캐시에서 조회)

**요청 예시:**
```
GET /api/market/prices/BTCUSDT
```

**응답: 200 OK**
```json
{
  "success": true,
  "data": {
    "symbol": "BTCUSDT",
    "bidPrice": "45999.50000000",
    "bidQty": "2.15000000",
    "askPrice": "46000.00000000",
    "askQty": "1.80000000",
    "timestamp": "2025-12-26T12:00:00Z"
  },
  "message": "성공",
  "timestamp": "2025-12-26T12:00:00Z"
}
```

**필드 설명:**
- `bidPrice`: 매수 호가 (살 수 있는 최고 가격)
- `askPrice`: 매도 호가 (팔 수 있는 최저 가격)
- `bidQty`: 매수 호가 수량
- `askQty`: 매도 호가 수량

**에러:**
- `404` - 지원하지 않는 심볼
- `503` - 가격 데이터 없음 (WebSocket 연결 끊김)

---

### 2. 지원 심볼 목록

**GET** `/api/market/symbols`

지원하는 거래 페어 목록을 조회합니다.

**응답: 200 OK**
```json
{
  "success": true,
  "data": [
    {
      "symbol": "BTCUSDT",
      "baseAsset": "BTC",
      "quoteAsset": "USDT",
      "status": "TRADING",
      "minQuantity": "0.00001000",
      "maxQuantity": "9000.00000000",
      "stepSize": "0.00001000"
    },
    {
      "symbol": "ETHUSDT",
      "baseAsset": "ETH",
      "quoteAsset": "USDT",
      "status": "TRADING",
      "minQuantity": "0.00100000",
      "maxQuantity": "90000.00000000",
      "stepSize": "0.00100000"
    }
  ],
  "message": "성공",
  "timestamp": "2025-12-26T12:00:00Z"
}
```

---

### 3. 실시간 가격 스트림 (WebSocket)

**향후 구현 예정**

클라이언트가 실시간으로 가격 업데이트를 받을 수 있는 WebSocket 엔드포인트입니다.

**연결:**
```
ws://localhost:8080/ws/market/prices
```

**구독 메시지:**
```json
{
  "action": "subscribe",
  "symbols": ["BTCUSDT", "ETHUSDT"]
}
```

**서버 → 클라이언트 메시지:**
```json
{
  "type": "priceUpdate",
  "symbol": "BTCUSDT",
  "bidPrice": "46000.00000000",
  "askPrice": "46000.50000000",
  "timestamp": "2025-12-26T12:00:01Z"
}
```

---

## 에러 코드

### 인증 에러

| 에러 코드 | HTTP 상태 | 메시지 | 설명 |
|---------|----------|--------|------|
| `INVALID_CREDENTIALS` | 401 | 잘못된 username 또는 password | 로그인 실패 |
| `TOKEN_EXPIRED` | 401 | 토큰이 만료되었습니다 | JWT 토큰 만료 |
| `TOKEN_INVALID` | 401 | 유효하지 않은 토큰 | 잘못된 JWT 형식 |
| `UNAUTHORIZED` | 401 | 인증이 필요합니다 | 토큰 없음 |
| `FORBIDDEN` | 403 | 접근 권한이 없습니다 | 다른 사용자 리소스 접근 |

### 계좌 에러

| 에러 코드 | HTTP 상태 | 메시지 | 설명 |
|---------|----------|--------|------|
| `ACCOUNT_NOT_FOUND` | 404 | 계좌를 찾을 수 없습니다 | 존재하지 않는 계좌 |
| `INSUFFICIENT_BALANCE` | 409 | 잔고가 부족합니다 | 출금/주문 시 잔고 부족 |

### 주문 에러

| 에러 코드 | HTTP 상태 | 메시지 | 설명 |
|---------|----------|--------|------|
| `ORDER_NOT_FOUND` | 404 | 주문을 찾을 수 없습니다 | 존재하지 않는 주문 |
| `INVALID_SYMBOL` | 400 | 지원하지 않는 심볼입니다 | 잘못된 거래 페어 |
| `INVALID_QUANTITY` | 400 | 주문 수량이 유효하지 않습니다 | 음수, 0, 너무 작은 값 |
| `INVALID_PRICE` | 400 | 주문 가격이 유효하지 않습니다 | 음수, 0 |
| `INSUFFICIENT_POSITION` | 409 | 보유 수량이 부족합니다 | 매도 시 수량 부족 |
| `CONCURRENT_ORDER_PROCESSING` | 409 | 다른 주문 처리 중입니다 | 분산 락 획득 실패 |
| `CANNOT_CANCEL_FILLED_ORDER` | 400 | 이미 체결된 주문은 취소할 수 없습니다 | 주문 취소 불가 |

### 포지션 에러

| 에러 코드 | HTTP 상태 | 메시지 | 설명 |
|---------|----------|--------|------|
| `POSITION_NOT_FOUND` | 404 | 포지션을 찾을 수 없습니다 | 존재하지 않는 포지션 |

### 시장 가격 에러

| 에러 코드 | HTTP 상태 | 메시지 | 설명 |
|---------|----------|--------|------|
| `PRICE_NOT_AVAILABLE` | 503 | 가격 데이터를 사용할 수 없습니다 | WebSocket 연결 끊김 |
| `SYMBOL_NOT_SUPPORTED` | 404 | 지원하지 않는 심볼입니다 | 잘못된 심볼 |

### 일반 에러

| 에러 코드 | HTTP 상태 | 메시지 | 설명 |
|---------|----------|--------|------|
| `VALIDATION_ERROR` | 400 | 입력 데이터 검증 실패 | 필수 필드 누락, 형식 오류 |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류 | 예상하지 못한 에러 |
| `SERVICE_UNAVAILABLE` | 503 | 서비스를 사용할 수 없습니다 | DB/Redis 연결 실패 |
| `RATE_LIMIT_EXCEEDED` | 429 | 요청 한도를 초과했습니다 | 너무 많은 요청 |

---

## Rate Limiting

### 제한 정책

| 엔드포인트 패턴 | 제한 (요청/분) | 비고 |
|---------------|--------------|------|
| `/api/auth/login` | 5 | 무차별 대입 공격 방지 |
| `/api/orders` (POST) | 30 | 주문 스팸 방지 |
| `/api/market/prices/*` | 100 | 가격 조회 제한 |
| 기타 모든 엔드포인트 | 60 | 일반 요청 제한 |

### 응답 헤더

```http
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1640520000
```

### Rate Limit 초과 응답

**429 Too Many Requests**
```json
{
  "success": false,
  "data": null,
  "message": "요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.",
  "timestamp": "2025-12-26T12:00:00Z",
  "errorCode": "RATE_LIMIT_EXCEEDED",
  "details": {
    "retryAfter": 30
  }
}
```

---

## 페이지네이션

모든 목록 조회 API는 페이지네이션을 지원합니다.

**쿼리 파라미터:**
- `page`: 페이지 번호 (0부터 시작, default=0)
- `size`: 페이지 크기 (default=20, max=100)
- `sort`: 정렬 기준 (예: `createdAt,desc`)

**응답 형식:**
```json
{
  "content": [ /* 데이터 배열 */ ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

---

## 테스트 데이터

### 개발 환경 테스트 계정

```
Username: testuser
Password: test1234!
Initial Balance: 10000 USDT
```

### 지원 심볼

- `BTCUSDT` - 비트코인/USDT
- `ETHUSDT` - 이더리움/USDT
- `BNBUSDT` - 바이낸스 코인/USDT

---

## Postman Collection

프로젝트 루트의 `postman/trade-api.postman_collection.json` 파일을 Postman에 import하여 사용할 수 있습니다.

**환경 변수:**
```json
{
  "baseUrl": "http://localhost:8080",
  "accessToken": "your_jwt_token_here"
}
```

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|-----|------|----------|
| 1.0.0 | 2025-12-26 | 초기 API 명세 작성 |
| 1.1.0 | 2025-01-12 | Account API 구현 완료 (생성, 조회) |
| 1.2.0 | (예정) | Market API 구현 (WebSocket 실시간 가격) |
| 1.3.0 | (예정) | Order API 구현 (주문 생성, 조회) |
| 1.4.0 | (예정) | Auth API 구현 (JWT 인증) |

---

## 참고 링크

- [CLAUDE.md](./CLAUDE.md) - 프로젝트 개요 및 코딩 가이드
- [ARCHITECTURE.md](./ARCHITECTURE.md) - 시스템 아키텍처 상세
- [DATABASE.md](./DATABASE.md) - 데이터베이스 스키마
- [Binance API 문서](https://binance-docs.github.io/apidocs/spot/en/)
