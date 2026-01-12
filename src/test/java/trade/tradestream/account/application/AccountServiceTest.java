package trade.tradestream.account.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trade.tradestream.account.domain.Account;
import trade.tradestream.account.domain.AccountNotFoundException;
import trade.tradestream.account.infra.AccountRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService 테스트")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Nested
    @DisplayName("createAccount() 메서드")
    class CreateAccountTest {

        @Test
        @DisplayName("계좌 생성 성공")
        void createAccount_Success() {
            // Given
            Long userId = 1L;
            BigDecimal initialBalance = new BigDecimal("10000");
            Account account = Account.create(userId, initialBalance);

            given(accountRepository.save(any(Account.class))).willReturn(account);

            // When
            Account result = accountService.createAccount(userId, initialBalance);

            // Then
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getBalance()).isEqualByComparingTo(initialBalance);
            verify(accountRepository).save(any(Account.class));
        }
    }

    @Nested
    @DisplayName("getAccount() 메서드")
    class GetAccountTest {

        @Test
        @DisplayName("ID로 계좌 조회 성공")
        void getAccount_Success() {
            // Given
            Long accountId = 1L;
            Account account = Account.create(1L, new BigDecimal("10000"));

            given(accountRepository.findById(accountId)).willReturn(Optional.of(account));

            // When
            Account result = accountService.getAccount(accountId);

            // Then
            assertThat(result).isEqualTo(account);
            verify(accountRepository).findById(accountId);
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 예외 발생")
        void getAccount_NotFound_ThrowsException() {
            // Given
            Long accountId = 999L;

            given(accountRepository.findById(accountId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> accountService.getAccount(accountId))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining("계좌를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("getAccountByUserId() 메서드")
    class GetAccountByUserIdTest {

        @Test
        @DisplayName("사용자 ID로 계좌 조회 성공")
        void getAccountByUserId_Success() {
            // Given
            Long userId = 1L;
            Account account = Account.create(userId, new BigDecimal("10000"));

            given(accountRepository.findByUserId(userId)).willReturn(Optional.of(account));

            // When
            Account result = accountService.getAccountByUserId(userId);

            // Then
            assertThat(result.getUserId()).isEqualTo(userId);
            verify(accountRepository).findByUserId(userId);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 조회 시 예외 발생")
        void getAccountByUserId_NotFound_ThrowsException() {
            // Given
            Long userId = 999L;

            given(accountRepository.findByUserId(userId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> accountService.getAccountByUserId(userId))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining("사용자 계좌를 찾을 수 없습니다");
        }
    }
}
