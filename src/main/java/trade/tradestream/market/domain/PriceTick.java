package trade.tradestream.market.domain;

import java.time.Instant;

public record PriceTick (

    String symbol,
    double price,
    Instant eventTime
) {}
