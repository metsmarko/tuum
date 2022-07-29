package ee.metsmarko.tuum.account;

import java.math.BigDecimal;

public final class AccountBalance {
  private final String currency;
  private final BigDecimal balance;

  public AccountBalance(String currency, BigDecimal balance) {
    this.currency = currency;
    this.balance = balance;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getBalance() {
    return balance;
  }
}
