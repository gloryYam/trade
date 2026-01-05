package trade.tradestream.account.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trade.tradestream.account.application.AccountService;
import trade.tradestream.account.domain.Account;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<Account> createAccount(
            @RequestParam Long userId,  // 쿼리 파라미터: ?userId=1
            @RequestParam BigDecimal initialBalance  // 쿼리 파라미터: ?balance=100000
    ) {
        // Service 호출
        Account account = accountService.createAccount(userId, initialBalance);

        // HTTP 200 OK + 계좌 정보 JSON 반환
        return ResponseEntity.ok(account);
    }

    /**
     * 계좌 조회 API (ID로)
     *
     * [HTTP 메서드]
     * - GET: 리소스 조회
     *
     * [요청]
     * GET /api/accounts/1
     *
     * [경로 변수]
     * - @PathVariable: URL 경로에서 값 추출
     * - /accounts/{id} → id 파라미터로 매핑
     *
     * [Step 1에서는]
     * - 계좌 없어도 200 OK + null 반환 (의도적!)
     * - Step 3에서 404 Not Found로 변경 예정
     *
     * @param id 계좌 ID
     * @return 계좌 정보 (200 OK)
     */
    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable Long id) {
        Account account = accountService.getAccount(id);
        return ResponseEntity.ok(account);
    }

    /**
     * 계좌 조회 API (사용자 ID로)
     *
     * [요청]
     * GET /api/accounts/user/1
     *
     * [왜 /user/{userId}?]
     * - /accounts/{id}와 구분하기 위해
     * - 명확한 의미 전달: "user의 계좌"
     *
     * @param userId 사용자 ID
     * @return 계좌 정보 (200 OK)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Account> getAccountByUserId(@PathVariable Long userId) {
        Account account = accountService.getAccountByUserId(userId);
        return ResponseEntity.ok(account);
    }
}
