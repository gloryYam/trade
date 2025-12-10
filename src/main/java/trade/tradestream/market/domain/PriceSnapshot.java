package trade.tradestream.market.domain;

import lombok.Builder;
import lombok.Getter;
import trade.tradestream.market.infra.jpa.MarketPriceEntity;

import java.time.Instant;

@Getter
@Builder
public class PriceSnapshot {

    private final String symbol;
    private final double price;
    private final Instant updatedAt;

    public static PriceSnapshot from(MarketPriceEntity entity) {
        return PriceSnapshot.builder()
                .symbol(entity.getSymbol())
                .price(entity.getPrice())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
