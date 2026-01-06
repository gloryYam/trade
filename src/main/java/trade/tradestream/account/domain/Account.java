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
        validateUserId(userId);
        validateBalance(initialBalance);

        Account account = new Account();
        account.userId = userId;
        account.balance = initialBalance;
        return account;
    }

    /**
     * 잔고 증가
     * @param amount 증가시킬 금액 (양수만 허용)
     * @throws InvalidAccountException 음수 금액 입력 시
     */

    public void increaseBalance(BigDecimal amount) {
        validateAmount(amount);
        this.balance = this.balance.add(amount);
    }

    /**
     * 잔고 차감
     * @param amount 차감할 금액 (양수만 허용)
     * @throws InsufficientBalanceException 잔고 부족 시
     * @throws InvalidAccountException 음수 금액 입력 시
     */
    public void decreaseBalance(BigDecimal amount) {
        validateAmount(amount);

        if(this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(amount, this.balance);
        }
        this.balance = this.balance.subtract(amount);
    }

    // === 검증 메서드 === //

    private static void validateUserId(Long userId) {
        if(userId == null || userId <= 0) {
            throw new InvalidAccountException(String.format("유효하지 않은 사용자 ID 입니다. (userId: %s)", userId));
        }
    }

    private static void validateBalance(BigDecimal balance) {
        if(balance == null) {
            throw new InvalidAccountException("잔고는 필수값입니다.");
        }

        if(balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAccountException(String.format("잔고는 음수일 수 없습니다. (입력값: %s)", balance));
        }
    }

    private static void validateAmount(BigDecimal amount) {
        if(amount == null) {
            throw new InvalidAccountException("금액은 null 일 수 없습니다.");
        }

        if(amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAccountException("금액은 0보다 커야 합니다. 입력값: " + amount);
        }
    }
}
