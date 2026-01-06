package trade.tradestream.account.domain;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(Long accountId) {
        super(String.format("계좌를 찾을 수 없습니다. accountId = %d", accountId));
    }

    public AccountNotFoundException(String message) {
        super(message);
    }
}
