package ee.metsmarko.tuum.account.event;

import static ee.metsmarko.tuum.account.TransactionDirection.IN;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.metsmarko.tuum.account.Account;
import ee.metsmarko.tuum.account.CreateTransactionResponse;
import ee.metsmarko.tuum.account.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class AccountEventPublisher {
  private static final Logger log = LoggerFactory.getLogger(AccountEventPublisher.class);
  private final RabbitTemplate template;
  private final Exchange exchange;
  private final ObjectMapper mapper;

  private static final String NEW_ACCOUNT_KEY = "account.create";
  private static final String NEW_TX_KEY = "account.transaction.create";
  private static final String BALANCE_INCREASED_KEY = "account.balance.increase";
  private static final String BALANCE_DECREASED_KEY = "account.balance.decrease";

  public AccountEventPublisher(RabbitTemplate template, Exchange exchange, ObjectMapper mapper) {
    this.template = template;
    this.exchange = exchange;
    this.mapper = mapper;
  }

  public void accountCreated(Account account) {
    publishEvent(NEW_ACCOUNT_KEY, new AccountCreateEvent(account.getId()));
  }

  public void transactionCreated(Transaction transaction) {
    publishEvent(
        NEW_TX_KEY, new TransactionCreateEvent(transaction.getId(), transaction.getAccountId())
    );
  }

  public void balanceChanged(CreateTransactionResponse res) {
    publishEvent(
        res.direction() == IN ? BALANCE_INCREASED_KEY : BALANCE_DECREASED_KEY,
        new BalanceChangeEvent(res.accountId(), res.currency(), res.amount(), res.currentBalance())
    );
  }

  private void publishEvent(String key, Object event) {
    try {
      template.convertAndSend(exchange.getName(), key, mapper.writeValueAsBytes(event));
    } catch (JsonProcessingException e) {
      log.error("Error serializing event");
    }
  }
}
