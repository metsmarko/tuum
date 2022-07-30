package ee.metsmarko.tuum.account;

import static ee.metsmarko.tuum.account.TransactionDirection.IN;
import static ee.metsmarko.tuum.account.TransactionDirection.OUT;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ee.metsmarko.tuum.account.event.AccountEventPublisher;
import ee.metsmarko.tuum.exception.TuumException;
import ee.metsmarko.tuum.exception.TuumInvalidInputException;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AccountServiceTest {
  private final UUID accountId = UUID.randomUUID();
  private final AccountMapper accountMapper = mock(AccountMapper.class);
  private final AccountEventPublisher eventPublisher = mock(AccountEventPublisher.class);
  private final AccountService accountService = new AccountService(accountMapper, eventPublisher);
  private final Account account = new Account(
      accountId, UUID.randomUUID(), "EE",
      List.of(
          new AccountBalance("USD", BigDecimal.ZERO),
          new AccountBalance("EUR", BigDecimal.ZERO)
      )
  );

  @Test
  void testCreateAccount_Ok() throws TuumException {
    Account account = accountService.createAccount(
        new CreateAccountRequest(accountId, "EE", new LinkedHashSet<>(List.of("USD", "EUR")))
    );

    assertEquals(accountId, account.getCustomerId());
    assertEquals("EE", account.getCountry());
    assertEquals(BigDecimal.ZERO, account.getAccountBalances().get(0).getBalance());
    assertEquals("USD", account.getAccountBalances().get(0).getCurrency());

    assertEquals(BigDecimal.ZERO, account.getAccountBalances().get(1).getBalance());
    assertEquals("EUR", account.getAccountBalances().get(1).getCurrency());

    verify(accountMapper).createAccount(account);
    verify(accountMapper).createAccountBalance(account);
    verify(eventPublisher).accountCreated(eq(account));
  }

  @Test
  void testCreateAccount_WrongCurrency() {
    verifyNewAccountError(
        new CreateAccountRequest(UUID.randomUUID(), "EE", Set.of("EEK")),
        "invalid currency - EEK"
    );
  }

  @Test
  void testCreateAccount_MissingCurrency() {
    verifyNewAccountError(
        new CreateAccountRequest(UUID.randomUUID(), "EE", emptySet()),
        "currencies missing"
    );
    verifyNewAccountError(
        new CreateAccountRequest(UUID.randomUUID(), "EE", null),
        "currencies missing"
    );
  }

  @Test
  void testCreateAccount_MissingCustomerId() {
    verifyNewAccountError(
        new CreateAccountRequest(null, "EE", Set.of("EUR")),
        "customer id missing"
    );
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " "})
  void testCreateAccount_MissingCountry(String country) {
    verifyNewAccountError(
        new CreateAccountRequest(UUID.randomUUID(), country, Set.of("EUR")),
        "country missing"
    );
  }

  @Test
  void testCreateTransactionIn_Ok() throws TuumException {
    CreateTransactionRequest request =
        new CreateTransactionRequest(BigDecimal.TEN, "EUR", IN, "ice cream");
    Account account = new Account(
        accountId, UUID.randomUUID(), "EE", List.of(new AccountBalance("EUR", BigDecimal.ZERO))
    );
    Account accountAfter = new Account(
        accountId, UUID.randomUUID(), "EE", List.of(new AccountBalance("EUR", BigDecimal.TEN))
    );
    when(accountMapper.getAccountById(accountId)).thenReturn(account).thenReturn(accountAfter);

    CreateTransactionResponse transaction = accountService.createTransaction(accountId, request);

    verify(accountMapper).increaseBalance(eq(accountId), eq("EUR"), eq(BigDecimal.TEN));
    assertEquals(BigDecimal.TEN, transaction.currentBalance());
    verify(eventPublisher).transactionCreated(any());
    verify(eventPublisher).balanceChanged(transaction);
  }

  @Test
  void testCreateTransactionOut_Ok() throws TuumException {
    CreateTransactionRequest request =
        new CreateTransactionRequest(BigDecimal.TEN, "EUR", OUT, "ice cream");
    AccountBalance balance = new AccountBalance("EUR", BigDecimal.TEN);
    Account account = new Account(accountId, UUID.randomUUID(), "EE", List.of(balance));
    Account accountAfter = new Account(
        accountId, UUID.randomUUID(), "EE", List.of(new AccountBalance("EUR", BigDecimal.ZERO))
    );
    when(accountMapper.getAccountById(accountId)).thenReturn(account).thenReturn(accountAfter);
    when(accountMapper.getBalanceForUpdate(eq(accountId), eq("EUR"))).thenReturn(balance);

    CreateTransactionResponse transaction = accountService.createTransaction(accountId, request);

    verify(accountMapper).decreaseBalance(eq(accountId), eq("EUR"), eq(BigDecimal.TEN));
    assertEquals(BigDecimal.ZERO, transaction.currentBalance());
    verify(eventPublisher).transactionCreated(any());
    verify(eventPublisher).balanceChanged(transaction);
  }

  @Test
  void testCreateTransactionOut_InsufficientFunds() {
    CreateTransactionRequest request =
        new CreateTransactionRequest(BigDecimal.valueOf(10.01), "EUR", OUT, "ice cream");
    AccountBalance balance = new AccountBalance("EUR", BigDecimal.TEN);
    Account account = new Account(accountId, UUID.randomUUID(), "EE", List.of(balance));
    when(accountMapper.getAccountById(accountId)).thenReturn(account);
    when(accountMapper.getBalanceForUpdate(eq(accountId), eq("EUR"))).thenReturn(balance);

    TuumInvalidInputException ex = assertThrows(
        TuumInvalidInputException.class, () -> accountService.createTransaction(accountId, request)
    );

    verify(accountMapper, never()).decreaseBalance(any(), any(), any());
    assertEquals("insufficient funds", ex.getMessage());
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void testCreateTransaction_InvalidAccount() {
    verifyNewTransactionError(
        new CreateTransactionRequest(BigDecimal.TEN, "EUR", OUT, "ice cream"),
        "account is missing"
    );
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"EEK"})
  void testCreateTransaction_InvalidCurrency(String currency) {
    when(accountMapper.getAccountById(accountId)).thenReturn(account);
    verifyNewTransactionError(
        new CreateTransactionRequest(BigDecimal.TEN, currency, OUT, "ice cream"),
        "no balance for given currency"
    );
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " "})
  void testCreateTransaction_MissingDescription(String description) {
    when(accountMapper.getAccountById(accountId)).thenReturn(account);
    verifyNewTransactionError(
        new CreateTransactionRequest(BigDecimal.TEN, "EUR", OUT, description),
        "description is missing"
    );
  }

  @ParameterizedTest
  @ValueSource(doubles = {-1, -0.00001, 0})
  void testCreateTransaction_InvalidAmount(double amount) {
    when(accountMapper.getAccountById(accountId)).thenReturn(account);
    verifyNewTransactionError(
        new CreateTransactionRequest(BigDecimal.valueOf(amount), "EUR", OUT, "ice cream"),
        "invalid amount"
    );
  }

  @Test
  void testCreateTransaction_NullAmount() {
    when(accountMapper.getAccountById(accountId)).thenReturn(account);
    verifyNewTransactionError(
        new CreateTransactionRequest(null, "EUR", OUT, "ice cream"),
        "invalid amount"
    );
  }

  @Test
  void testCreateTransaction_InvalidDirection() {
    when(accountMapper.getAccountById(accountId)).thenReturn(account);
    verifyNewTransactionError(
        new CreateTransactionRequest(BigDecimal.TEN, "EUR", null, "ice cream"),
        "direction is missing"
    );
  }

  @Test
  public void getTransactions() throws Exception {
    when(accountMapper.getAccountById(accountId)).thenReturn(account);
    when(accountMapper.getTransactionsByAccountId(accountId)).thenReturn(List.of(
        mock(Transaction.class), mock(Transaction.class)
    ));

    List<Transaction> transactions = accountService.getTransactionsByAccountId(accountId);

    assertEquals(2, transactions.size());
  }

  @Test
  public void getTransactions_MissingAccount() {
    TuumInvalidInputException ex = assertThrows(
        TuumInvalidInputException.class, () -> accountService.getTransactionsByAccountId(accountId)
    );

    assertEquals("invalid account", ex.getMessage());
  }

  private void verifyNewTransactionError(CreateTransactionRequest request, String errorMessage) {
    TuumInvalidInputException ex = assertThrows(
        TuumInvalidInputException.class, () -> accountService.createTransaction(accountId, request)
    );

    assertEquals(errorMessage, ex.getMessage());
    verifyNoInteractions(eventPublisher);
  }

  private void verifyNewAccountError(CreateAccountRequest request, String errorMessage) {
    TuumInvalidInputException ex =
        assertThrows(TuumInvalidInputException.class, () -> accountService.createAccount(
            request
        ));

    assertEquals(errorMessage, ex.getMessage());
    verifyNoInteractions(eventPublisher);
  }
}
