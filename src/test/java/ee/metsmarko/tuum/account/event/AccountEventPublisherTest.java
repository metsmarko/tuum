package ee.metsmarko.tuum.account.event;


import static ee.metsmarko.tuum.account.TransactionDirection.IN;
import static ee.metsmarko.tuum.account.TransactionDirection.OUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.metsmarko.tuum.account.Account;
import ee.metsmarko.tuum.account.AccountBalance;
import ee.metsmarko.tuum.account.CreateTransactionResponse;
import ee.metsmarko.tuum.account.Transaction;
import ee.metsmarko.tuum.account.TransactionDirection;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class AccountEventPublisherTest {

  private final RabbitTemplate template = mock(RabbitTemplate.class);
  private final ObjectMapper mapper = new ObjectMapper();
  private final AccountEventPublisher publisher = new AccountEventPublisher(
      template,
      new TopicExchange("topic"),
      mapper
  );
  private static final String EXCHANGE_NAME = "topic";
  private final ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);

  @Test
  void testAccountCreated() throws IOException {
    Account account = new Account(
        UUID.randomUUID(), UUID.randomUUID(), "EE",
        List.of(new AccountBalance("EUR", BigDecimal.ZERO))
    );

    publisher.accountCreated(account);

    verify(template).convertAndSend(eq(EXCHANGE_NAME), eq("account.create"), captor.capture());
    AccountCreateEvent event = mapper.readValue(captor.getValue(), AccountCreateEvent.class);
    assertEquals(account.getId(), event.accountId());
  }

  @Test
  void testTransactionCreated() throws IOException {
    Transaction transaction = new Transaction(
        UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN,
        "EUR", IN, "desc");

    publisher.transactionCreated(transaction);

    verify(template).convertAndSend(
        eq(EXCHANGE_NAME), eq("account.transaction.create"), captor.capture()
    );
    TransactionCreateEvent event =
        mapper.readValue(captor.getValue(), TransactionCreateEvent.class);
    assertEquals(transaction.getId(), event.transactionId());
    assertEquals(transaction.getAccountId(), event.accountId());
  }

  @Test
  void testBalanceIncreased() throws IOException {
    verifyBalanceChange("account.balance.increase", IN);
  }

  @Test
  void testBalanceDecreased() throws IOException {
    verifyBalanceChange("account.balance.decrease", OUT);
  }

  private void verifyBalanceChange(String key, TransactionDirection direction) throws IOException {
    CreateTransactionResponse transaction = new CreateTransactionResponse(
        UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ONE,
        "EUR", direction, "desc", BigDecimal.TEN);

    publisher.balanceChanged(transaction);

    verify(template).convertAndSend(
        eq(EXCHANGE_NAME), eq(key), captor.capture()
    );
    BalanceChangeEvent event = mapper.readValue(captor.getValue(), BalanceChangeEvent.class);
    assertEquals(transaction.accountId(), event.accountId());
    assertEquals(transaction.amount(), event.amount());
    assertEquals(transaction.currentBalance(), event.newBalance());
    assertEquals(transaction.currency(), event.currency());
  }
}
