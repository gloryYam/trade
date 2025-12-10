package trade.tradestream.market.application;

import lombok.Getter;

@Getter
public class PriceRequestService {

    private String symbol;

    public PriceRequestService(String symbol) {
        this.symbol = symbol;
    }
}
