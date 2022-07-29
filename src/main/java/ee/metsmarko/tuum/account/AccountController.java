package ee.metsmarko.tuum.account;

import ee.metsmarko.tuum.exception.TuumException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {
  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @PostMapping
  public Account createAccount(@RequestBody CreateAccountRequest createAccountRequest)
      throws TuumException {
    return accountService.createAccount(createAccountRequest);
  }

  @GetMapping("/{accountId}")
  public ResponseEntity<Account> getAccount(@PathVariable UUID accountId) {
    return accountService.getAccountById(accountId)
        .map(a -> ResponseEntity.ok().body(a))
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/{accountId}/transactions")
  public List<Transaction> getTransactions(@PathVariable UUID accountId) throws TuumException {
    return accountService.getTransactionsByAccountId(accountId);
  }

  @PostMapping("/{accountId}/transaction")
  public CreateTransactionResponse createTransaction(
      @PathVariable UUID accountId, @RequestBody CreateTransactionRequest createTransactionRequest
  ) throws TuumException {
    return accountService.createTransaction(accountId, createTransactionRequest);
  }
}
