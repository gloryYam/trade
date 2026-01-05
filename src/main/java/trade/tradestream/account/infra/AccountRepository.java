package trade.tradestream.account.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import trade.tradestream.account.domain.Account;

import java.util.Optional;

/**
 * [역할]
 *  - Account 엔티티의 DB CRUD 담당
 *  - JPA 를 통한 데이터 영속성 관리
 */
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUserId(Long userId);
}
