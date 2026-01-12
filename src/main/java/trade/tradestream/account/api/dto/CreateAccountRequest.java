package trade.tradestream.account.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateAccountRequest(

        @NotNull(message = "사용자 ID는 필수입니다.")
        @Positive(message = "사용자 ID는 양수여야 합니다.")
        Long userId,

        @NotNull(message = "초기 잔고는 필수입니다.")
        @Positive(message = "초기 잔고는 0보다 커야 합니다.")
        BigDecimal initialBalance
) {
}
