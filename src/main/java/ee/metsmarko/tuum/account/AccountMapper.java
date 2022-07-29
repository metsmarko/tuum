package ee.metsmarko.tuum.account;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {

  void createAccount(Account account);

  void createAccountBalance(Account accountBalance);

  Account getAccountById(UUID id);

  void increaseBalance(
      @Param("accountId") UUID accountId, @Param("currency") String currency,
      @Param("amount") BigDecimal amount
  );

  void decreaseBalance(
      @Param("accountId") UUID accountId, @Param("currency") String currency,
      @Param("amount") BigDecimal amount
  );

  void createTransaction(Transaction transaction);

  List<Transaction> getTransactionsByAccountId(UUID accountId);

  AccountBalance getBalanceForUpdate(@Param("accountId") UUID accountId,
                                     @Param("currency") String currency);
}
