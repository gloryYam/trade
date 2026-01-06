package trade.tradestream.account.domain;

import java.math.BigDecimal;

/**
 * 잔고 부족 예외
 */
public class InsufficientBalanceException extends RuntimeException{

    public InsufficientBalanceException (BigDecimal required, BigDecimal actual) {
        super(String.format("잔고가 부족합니다. 필요 금액: %s, 현재 잔고: %s", required, actual));
    }

    public InsufficientBalanceException (String message) {
        super(message);
    }
}
