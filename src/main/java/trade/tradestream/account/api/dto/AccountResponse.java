package trade.tradestream.account.api.dto;

import trade.tradestream.account.domain.Account;

import java.math.BigDecimal;

/**
 * 계좌 조회 응답 DTO
 */
public record AccountResponse(
        Long id,
        Long userId,
        BigDecimal balance
) {

    // Entity -> DTO 변환
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getUserId(),
                account.getBalance()
        );
    }
}
