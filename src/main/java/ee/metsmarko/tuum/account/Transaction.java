package ee.metsmarko.tuum.account;

import java.math.BigDecimal;
import java.util.UUID;

public class Transaction {
  private final UUID id;
  private final UUID accountId;
  private final BigDecimal amount;
  private final String currency;
  private final TransactionDirection direction;
  private final String description;

  public Transaction(
      UUID id, UUID accountId, BigDecimal amount,
      String currency, TransactionDirection direction, String description
  ) {
    this.id = id;
    this.accountId = accountId;
    this.amount = amount;
    this.currency = currency;
    this.direction = direction;
    this.description = description;
  }

  public UUID getId() {
    return id;
  }

  public UUID getAccountId() {
    return accountId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public TransactionDirection getDirection() {
    return direction;
  }

  public String getDescription() {
    return description;
  }
}
