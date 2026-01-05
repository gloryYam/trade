package trade.tradestream.account.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import trade.tradestream.account.domain.Account;
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
     */
    public Account getAccount(Long accountId) {

        // Optional.orElse(null): 값이 없으면 null 반환
        // Step 3에서는 orElseThrow()로 변경 예정
        return accountRepository.findById(accountId).orElse(null);
    }

    /**
     * 사용자 ID로 계좌 조회
     */
    public Account getAccountByUserId(Long userId) {
        return accountRepository.findByUserId(userId).orElse(null);
    }
}
