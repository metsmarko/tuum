package ee.metsmarko.tuum.account;

import ee.metsmarko.tuum.account.event.AccountEventPublisher;
import ee.metsmarko.tuum.exception.TuumException;
import ee.metsmarko.tuum.exception.TuumInvalidInputException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
  private static final Logger log = LoggerFactory.getLogger(AccountService.class);
  private static final Set<String> ALLOWED_CURRENCIES = Set.of("EUR", "SEK", "GBP", "USD");

  private final AccountMapper accountMapper;
  private final AccountEventPublisher eventPublisher;

  public AccountService(AccountMapper accountMapper, AccountEventPublisher eventPublisher) {
    this.accountMapper = accountMapper;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Creates a new account and balances for each currency. Balances are initialized to 0.
   *
   * @param createAccountRequest account data
   * @throws TuumException when validation fails
   */
  @Transactional
  public Account createAccount(CreateAccountRequest createAccountRequest) throws TuumException {
    log.info("Create account: {}", createAccountRequest);
    validateNewAccountInput(createAccountRequest);
    List<AccountBalance> balances = createAccountRequest.currencies().stream()
        .map(c -> new AccountBalance(c, BigDecimal.ZERO)).toList();
    Account account = new Account(
        null, createAccountRequest.customerId(), createAccountRequest.country(), balances
    );
    accountMapper.createAccount(account);
    accountMapper.createAccountBalance(account);
    eventPublisher.accountCreated(account);
    return account;
  }

  /**
   * Returns account by its id.
   *
   * @param id id of the account
   * @return an Optional containing account if found, empty Optional otherwise
   */
  public Optional<Account> getAccountById(UUID id) {
    return Optional.ofNullable(accountMapper.getAccountById(id));
  }

  /**
   * Creates a new transaction and updates account balance for given currency.
   *
   * @param accountId                account id on which transaction is performed
   * @param createTransactionRequest transaction data
   * @return created transaction with updated account balance for given currency
   * @throws TuumException when validation fails
   */
  @Transactional
  public CreateTransactionResponse createTransaction(
      UUID accountId, CreateTransactionRequest createTransactionRequest
  ) throws TuumException {
    log.info("Create transaction for user {}: {}", accountId, createTransactionRequest);
    Transaction transaction = mapToTransaction(accountId, createTransactionRequest);
    validateTransaction(getAccountById(accountId), transaction);
    switch (transaction.getDirection()) {
      case IN -> increaseBalance(transaction);
      case OUT -> decreaseBalance(transaction);
      default -> throw new TuumInvalidInputException("invalid direction");
    }
    accountMapper.createTransaction(transaction);
    CreateTransactionResponse createTransactionResponse =
        createResponse(transaction, accountMapper.getAccountById(accountId));
    eventPublisher.transactionCreated(transaction);
    eventPublisher.balanceChanged(createTransactionResponse);
    return createTransactionResponse;
  }

  private void increaseBalance(Transaction transaction) {
    accountMapper.increaseBalance(transaction.getAccountId(), transaction.getCurrency(),
        transaction.getAmount());
  }

  private void decreaseBalance(Transaction transaction) throws TuumInvalidInputException {
    AccountBalance accountBalance = accountMapper
        .getBalanceForUpdate(transaction.getAccountId(), transaction.getCurrency());
    if (isInsufficientFunds(transaction, accountBalance)) {
      throw new TuumInvalidInputException("insufficient funds");
    }
    accountMapper.decreaseBalance(transaction.getAccountId(), transaction.getCurrency(),
        transaction.getAmount());
  }

  /**
   * Returns all transactions for account.
   *
   * @param accountId id of the account
   * @return List of transactions, no paging.
   * @throws TuumException when account does not exist
   */
  public List<Transaction> getTransactionsByAccountId(UUID accountId) throws TuumException {
    if (getAccountById(accountId).isEmpty()) {
      throw new TuumInvalidInputException("invalid account");
    }
    return accountMapper.getTransactionsByAccountId(accountId);
  }

  private boolean isInsufficientFunds(Transaction transaction, AccountBalance accountBalance) {
    return accountBalance.getBalance()
        .subtract(transaction.getAmount())
        .compareTo(BigDecimal.ZERO) < 0;
  }

  private Transaction mapToTransaction(UUID accountId,
                                       CreateTransactionRequest createTransactionRequest) {
    return new Transaction(
        null, accountId, createTransactionRequest.amount(),
        createTransactionRequest.currency(), createTransactionRequest.direction(),
        createTransactionRequest.description()
    );
  }

  private CreateTransactionResponse createResponse(Transaction transaction, Account account) {
    BigDecimal newBalance = findBalanceByCurrency(account, transaction.getCurrency())
        .map(AccountBalance::getBalance).get(); // must be present
    return new CreateTransactionResponse(
        transaction.getId(), transaction.getAccountId(), transaction.getAmount(),
        transaction.getCurrency(), transaction.getDirection(),
        transaction.getDescription(), newBalance
    );
  }

  private void validateTransaction(Optional<Account> account, Transaction transaction)
      throws TuumInvalidInputException {
    if (account.isEmpty()) {
      throw new TuumInvalidInputException("account is missing");
    }
    if (findBalanceByCurrency(account.get(), transaction.getCurrency()).isEmpty()) {
      throw new TuumInvalidInputException("no balance for given currency");
    }
    if (transaction.getDescription() == null || transaction.getDescription().isBlank()) {
      throw new TuumInvalidInputException("description is missing");
    }
    if (transaction.getAmount() == null
        || transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new TuumInvalidInputException("invalid amount");
    }
    if (transaction.getDirection() == null) {
      throw new TuumInvalidInputException("direction is missing");
    }
  }

  private Optional<AccountBalance> findBalanceByCurrency(Account account, String currency) {
    return account.getAccountBalances().stream().filter(it -> it.getCurrency().equals(currency))
        .findFirst();
  }

  private void validateNewAccountInput(CreateAccountRequest account)
      throws TuumInvalidInputException {
    if (account.currencies() == null || account.currencies().isEmpty()) {
      throw new TuumInvalidInputException("currencies missing");
    }
    for (String currency : account.currencies()) {
      if (!ALLOWED_CURRENCIES.contains(currency)) {
        log.error("Invalid currency - {}", currency);
        throw new TuumInvalidInputException("invalid currency - " + currency);
      }
    }
    if (account.customerId() == null) {
      throw new TuumInvalidInputException("customer id missing");
    }
    if (account.country() == null || account.country().isBlank()) {
      throw new TuumInvalidInputException("country missing");
    }
  }
}
