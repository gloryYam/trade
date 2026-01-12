package trade.tradestream.account.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trade.tradestream.account.api.dto.AccountResponse;
import trade.tradestream.account.api.dto.CreateAccountRequest;
import trade.tradestream.account.application.AccountService;
import trade.tradestream.account.domain.Account;
import trade.tradestream.common.dto.ApiResponse;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ApiResponse<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        // Service 호출
        Account account = accountService.createAccount(request.userId(), request.initialBalance());

        // HTTP 200 OK + 계좌 정보 JSON 반환
        return ApiResponse.success(AccountResponse.from(account));
    }

    /**
     * 계좌 조회 API (ID로)
     */
    @GetMapping("/{id}")
    public ApiResponse<AccountResponse> getAccount(@PathVariable Long id) {
        Account account = accountService.getAccount(id);
        return ApiResponse.success(AccountResponse.from(account));

    }

    /**
     * 계좌 조회 API (사용자 ID로)
     */
    @GetMapping("/user/{userId}")
    public ApiResponse<AccountResponse> getAccountByUserId(@PathVariable Long userId) {
        Account account = accountService.getAccountByUserId(userId);
        return ApiResponse.success(AccountResponse.from(account));
    }
}
