package trade.tradestream.account.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trade.tradestream.account.domain.Account;
import trade.tradestream.account.domain.AccountNotFoundException;
import trade.tradestream.account.infra.AccountRepository;

import java.math.BigDecimal;

/**
 *
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public Account createAccount(Long userId, BigDecimal initialBalance) {

        // 엔티티 생성
        Account account = Account.create(userId, initialBalance);

        // DB에 저장
        return accountRepository.save(account);
    }

    /**
     * ID로 계좌 조회
     * @throws AccountNotFoundException 계좌가 존재하지 않을 때
     */
    @Transactional(readOnly = true)
    public Account getAccount(Long accountId) {

        // Optional.orElse(null): 값이 없으면 null 반환
        // Step 3에서는 orElseThrow()로 변경 예정
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    /**
     * 사용자 ID로 계좌 조회1
     */
    @Transactional(readOnly = true)
    public Account getAccountByUserId(Long userId) {
        return accountRepository.findByUserId(userId).orElseThrow(
            () -> new AccountNotFoundException(String.format("사용자 계좌를 찾을 수 없습니다. userId = %d", userId)));
    }
}
