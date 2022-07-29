package ee.metsmarko.tuum.account;

import java.util.List;
import java.util.UUID;

public class Account {
  private UUID id;
  private UUID customerId;
  private String country;
  private List<AccountBalance> accountBalances;

  private Account() {
    // required by MyBatis as it is not able to instantiate immutable objects that have collections
    // via constructor injection
  }

  public Account(UUID id, UUID customerId, String country, List<AccountBalance> accountBalances) {
    this.id = id;
    this.customerId = customerId;
    this.country = country;
    this.accountBalances = accountBalances;
  }

  public UUID getId() {
    return id;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public String getCountry() {
    return country;
  }

  public List<AccountBalance> getAccountBalances() {
    return accountBalances;
  }
}
