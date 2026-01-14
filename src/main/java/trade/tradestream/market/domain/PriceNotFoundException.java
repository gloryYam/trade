package trade.tradestream.market.domain;

public class PriceNotFoundException extends RuntimeException{

    public PriceNotFoundException(String symbol) {
        super(String.format("가격 정보를 찾을 수 없습니다. symbol = %s", symbol));
    }
}
