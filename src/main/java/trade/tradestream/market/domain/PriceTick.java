package trade.tradestream.market.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 실시간 가격 정보 (Binance bookTicker 기반)
 *
 * [ Walking Skeleton]
 * - DB 저장 안 함 → Entity 아님, Redis 캐시 전용
 * - Record 사용 → 불변성 보장, Getter 자동 생성
 * - 검증 없음 → 필요하면 추가
 * - 메서드 없음 → 최소 구현, 필요하면 추가
 */
public record PriceTick(
        String symbol,          // 거래 페어 (예: BTCUSDT)
        BigDecimal bidPrice,    // 매수 호가 (살 수 있는 최고 가격)
        BigDecimal askPrice,    // 매도 호가 (팔 수 있는 최저 가격)
        Instant timestamp       // 수신 시각
) {
}
