package trade.tradestream.market.infra;


import trade.tradestream.market.domain.PriceTick;

import java.util.Optional;

/**
 * 최신 가격 저장소 인터페이스
 *
 * [의도]
 * - 인터페이스로 추상화 → Redis 외 다른 저장소로 교체 가능
 * - application 레이어에서 infra 구현체에 직접 의존 안 함
 * - DIP(의존성 역전) 원칙 적용
 */

public interface LatestPriceStore {

    void save(PriceTick priceTick);

    Optional<PriceTick> findBySymbol(String symbol);
}
