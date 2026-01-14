package trade.tradestream.market.api;

import trade.tradestream.market.domain.PriceTick;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceResponse(
        String symbol,
        BigDecimal bidPrice,
        BigDecimal askPrice,
        Instant timestamp
) {

    public static PriceResponse from(PriceTick priceTick) {
        return new PriceResponse(
                priceTick.symbol(),
                priceTick.bidPrice(),
                priceTick.askPrice(),
                priceTick.timestamp()
        );
    }
}
