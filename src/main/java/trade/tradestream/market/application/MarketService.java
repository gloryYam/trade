package trade.tradestream.market.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import trade.tradestream.market.domain.PriceNotFoundException;
import trade.tradestream.market.domain.PriceTick;
import trade.tradestream.market.infra.LatestPriceStore;

@Service
@RequiredArgsConstructor
public class MarketService {

    private final LatestPriceStore latestPriceStore;

    /**
     * 심볼의 최신 가격 조회
     * @throws PriceNotFoundException 가격 정보가 없을 때
     */
    public PriceTick getLatestPrice(String symbol) {
        String upperSymbol = symbol.toUpperCase();
        return latestPriceStore.findBySymbol(upperSymbol)
                .orElseThrow(() -> new PriceNotFoundException(symbol));
    }

    /**
     * 가격 저장 (WebSocket에서 호출)
     */
    public void savePrice(PriceTick priceTick) {
        latestPriceStore.save(priceTick);
    }
}
