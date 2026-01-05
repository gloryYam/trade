package trade.tradestream.account.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 계좌 소유자 ID
     * 추후 User 엔티티 생성 시 @ManyToOne 연관관계로 변경 예정
     */
    @Column(nullable = false, unique = true)
    private Long userId;

    /**
     * 계좌 잔고 (KRW 기준)
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    /**
     *  - 단순히 값만 할당
     *  - 검증 없음 (음수도 저장 가능 - 의도적!)
     *  검증 로직 추가 예정
     * @param userId
     * @param initialBalance
     * @return
     */
    public static Account create(Long userId, BigDecimal initialBalance) {
        Account account = new Account();
        account.userId = userId;
        account.balance = initialBalance;
        return account;
    }
}
