package trade.tradestream.account.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Account 엔티티 테스트")
class AccountTest {

    @Nested
    @DisplayName("create() 메서드")
    class CreateTest {

        @Test
        @DisplayName("정상적인 값으로 계좌 생성 성공")
        void create_Success() {
            // Given
            Long userId = 1L;
            BigDecimal initialBalance = new BigDecimal("10000.00");

            // When
            Account account = Account.create(userId, initialBalance);

            // Then
            assertThat(account.getUserId()).isEqualTo(userId);
            assertThat(account.getBalance()).isEqualByComparingTo(initialBalance);
        }

        @Test
        @DisplayName("잔고가 0인 계좌 생성 성공")
        void create_WithZeroBalance_Success() {
            // Given
            Long userId = 1L;
            BigDecimal zeroBalance = BigDecimal.ZERO;

            // When
            Account account = Account.create(userId, zeroBalance);

            // Then
            assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("userId가 null이면 예외 발생")
        void create_NullUserId_ThrowsException() {
            // Given
            Long nullUserId = null;
            BigDecimal balance = new BigDecimal("10000");

            // When & Then
            assertThatThrownBy(() -> Account.create(nullUserId, balance))
                    .isInstanceOf(InvalidAccountException.class)
                    .hasMessageContaining("유효하지 않은 사용자 ID");
        }

        @Test
        @DisplayName("userId가 0이면 예외 발생")
        void create_ZeroUserId_ThrowsException() {
            // Given
            Long zeroUserId = 0L;
            BigDecimal balance = new BigDecimal("10000");

            // When & Then
            assertThatThrownBy(() -> Account.create(zeroUserId, balance))
                    .isInstanceOf(InvalidAccountException.class)
                    .hasMessageContaining("유효하지 않은 사용자 ID");
        }

        @Test
        @DisplayName("userId가 음수이면 예외 발생")
        void create_NegativeUserId_ThrowsException() {
            // Given
            Long negativeUserId = -1L;
            BigDecimal balance = new BigDecimal("10000");

            // When & Then
            assertThatThrownBy(() -> Account.create(negativeUserId, balance))
                    .isInstanceOf(InvalidAccountException.class)
                    .hasMessageContaining("유효하지 않은 사용자 ID");
        }

        @Test
        @DisplayName("잔고가 null이면 예외 발생")
        void create_NullBalance_ThrowsException() {
            // Given
            Long userId = 1L;
            BigDecimal nullBalance = null;

            // When & Then
            assertThatThrownBy(() -> Account.create(userId, nullBalance))
                    .isInstanceOf(InvalidAccountException.class)
                    .hasMessageContaining("잔고는 필수값");
        }

        @Test
        @DisplayName("잔고가 음수이면 예외 발생")
        void create_NegativeBalance_ThrowsException() {
            // Given
            Long userId = 1L;
            BigDecimal negativeBalance = new BigDecimal("-100");

            // When & Then
            assertThatThrownBy(() -> Account.create(userId, negativeBalance))
                    .isInstanceOf(InvalidAccountException.class)
                    .hasMessageContaining("잔고는 음수일 수 없습니다");
        }
    }

    @Nested
    @DisplayName("increaseBalance() 메서드")
    class IncreaseBalanceTest {

        @Test
        @DisplayName("잔고 증가 성공")
        void increaseBalance_Success() {
            // Given
            Account account = Account.create(1L, new BigDecimal("10000"));
            BigDecimal amount = new BigDecimal("5000");

            // When
            account.increaseBalance(amount);

            // Then
            assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("15000"));
        }

        @Test
        @DisplayName("금액이 null이면 예외 발생")
        void increaseBalance_NullAmount_ThrowsException() {
            // Given
            Account account = Account.create(1L, new BigDecimal("10000"));

            // When & Then
            assertThatThrownBy(() -> account.increaseBalance(null))
                    .isInstanceOf(InvalidAccountException.class)
                    .hasMessageContaining("null");
        }

        @Test
        @DisplayName("금액이 0이면 예외 발생")
        void increaseBalance_ZeroAmount_ThrowsException() {
            // Given
            Account account = Account.create(1L, new BigDecimal("10000"));

            // When & Then
            assertThatThrownBy(() -> account.increaseBalance(BigDecimal.ZERO))
                    .isInstanceOf(InvalidAccountException.class)
                    .hasMessageContaining("0보다 커야");
        }

        @Test
        @DisplayName("금액이 음수이면 예외 발생")
        void increaseBalance_NegativeAmount_ThrowsException() {
            // Given
            Account account = Account.create(1L, new BigDecimal("10000"));
            BigDecimal negativeAmount = new BigDecimal("-100");

            // When & Then
            assertThatThrownBy(() -> account.increaseBalance(negativeAmount))
                    .isInstanceOf(InvalidAccountException.class)
                    .hasMessageContaining("0보다 커야");
        }
    }

    @Nested
    @DisplayName("decreaseBalance() 메서드")
    class DecreaseBalanceTest {

        @Test
        @DisplayName("잔고 차감 성공")
        void decreaseBalance_Success() {
            // Given
            Account account = Account.create(1L, new BigDecimal("10000"));
            BigDecimal amount = new BigDecimal("3000");

            // When
            account.decreaseBalance(amount);

            // Then
            assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("7000"));
        }

        @Test
        @DisplayName("잔고 전액 차감 성공")
        void decreaseBalance_AllBalance_Success() {
            // Given
            Account account = Account.create(1L, new BigDecimal("10000"));
            BigDecimal amount = new BigDecimal("10000");

            // When
            account.decreaseBalance(amount);

            // Then
            assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("잔고 부족 시 예외 발생")
        void decreaseBalance_InsufficientBalance_ThrowsException() {
            // Given
            Account account = Account.create(1L, new BigDecimal("10000"));
            BigDecimal amount = new BigDecimal("15000");

            // When & Then
            assertThatThrownBy(() -> account.decreaseBalance(amount))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessageContaining("잔고가 부족");
        }

        @Test
        @DisplayName("금액이 null이면 예외 발생")
        void decreaseBalance_NullAmount_ThrowsException() {
            // Given
            Account account = Account.create(1L, new BigDecimal("10000"));

            // When & Then
            assertThatThrownBy(() -> account.decreaseBalance(null))
                    .isInstanceOf(InvalidAccountException.class)
                    .hasMessageContaining("null");
        }

        @Test
        @DisplayName("금액이 0이면 예외 발생")
        void decreaseBalance_ZeroAmount_ThrowsException() {
            // Given
            Account account = Account.create(1L, new BigDecimal("10000"));

            // When & Then
            assertThatThrownBy(() -> account.decreaseBalance(BigDecimal.ZERO))
                    .isInstanceOf(InvalidAccountException.class)
                    .hasMessageContaining("0보다 커야");
        }

        @Test
        @DisplayName("금액이 음수이면 예외 발생")
        void decreaseBalance_NegativeAmount_ThrowsException() {
            // Given
            Account account = Account.create(1L, new BigDecimal("10000"));
            BigDecimal negativeAmount = new BigDecimal("-100");

            // When & Then
            assertThatThrownBy(() -> account.decreaseBalance(negativeAmount))
                    .isInstanceOf(InvalidAccountException.class)
                    .hasMessageContaining("0보다 커야");
        }
    }
}
