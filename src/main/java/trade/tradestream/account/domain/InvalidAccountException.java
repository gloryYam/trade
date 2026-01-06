package trade.tradestream.account.domain;

/**
 * 계좌 유효성 예외 (음수 잔고, null 값 등)
 */
public class InvalidAccountException extends RuntimeException{

    public InvalidAccountException(String message) {
        super(message);
    }

    public InvalidAccountException(String message, Throwable cause) {
        super(message, cause);
    }
}
