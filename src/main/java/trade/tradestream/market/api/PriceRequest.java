package trade.tradestream.market.api;

import trade.tradestream.market.application.PriceRequestService;

public class PriceRequest {

    private String symbol;

    public PriceRequest(String symbol) {
        this.symbol = symbol;
    }

    public PriceRequestService toPriceRequestService() {
        return new PriceRequestService(symbol);
    }
}
