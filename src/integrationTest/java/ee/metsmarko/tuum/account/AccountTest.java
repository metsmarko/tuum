package ee.metsmarko.tuum.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import ee.metsmarko.tuum.account.event.AccountCreateEvent;
import ee.metsmarko.tuum.account.event.BalanceChangeEvent;
import ee.metsmarko.tuum.account.event.TransactionCreateEvent;
import ee.metsmarko.tuum.exception.ErrorResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@ContextConfiguration(initializers = {AccountTest.Initializer.class})
@AutoConfigureMockMvc
@Import(RabbitMqTestConf.class)
public class AccountTest {

  @Autowired
  private ObjectMapper mapper;
  @Autowired
  private RabbitTemplate template;
  private static final PostgreSQLContainer<?> pgContainer = new PostgreSQLContainer<>("postgres:14")
      .withDatabaseName("tuum-db")
      .withUsername("tuum-user")
      .withPassword("tuum-pw");

  private static final RabbitMQContainer mqContainer = new RabbitMQContainer("rabbitmq:3.10.6");

  static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of(
          "spring.datasource.url=" + pgContainer.getJdbcUrl(),
          "spring.datasource.username=" + pgContainer.getUsername(),
          "spring.datasource.password=" + pgContainer.getPassword(),
          "spring.rabbitmq.host=localhost",
          "spring.rabbitmq.port=" + mqContainer.getMappedPort(5672),
          "spring.rabbitmq.username=guest",
          "spring.rabbitmq.password=guest"
      ).applyTo(configurableApplicationContext.getEnvironment());
    }
  }

  @Autowired
  private Queue queue;

  @BeforeAll
  static void beforeAll() {
    pgContainer.start();
    mqContainer.start();
  }

  @Autowired
  private MockMvc mockMvc;

  @Test
  void testGetMissingAccount() throws Exception {
    mockMvc
        .perform(get("/api/account/{id}", UUID.randomUUID()))
        .andExpect(status().isNotFound());
  }

  @Test
  void testGetTransactionsForInvalidAccount() throws Exception {
    MvcResult result = mockMvc
        .perform(get("/api/account/{id}/transactions", UUID.randomUUID()))
        .andExpect(status().isBadRequest()).andReturn();

    ErrorResponse error =
        mapper.readValue(result.getResponse().getContentAsByteArray(), ErrorResponse.class);
    assertEquals("invalid account", error.error());
  }

  @Test
  void testCreateAccountAndTransactions() throws Exception {
    Account account = createAccountAndVerify(
        new CreateAccountRequest(UUID.randomUUID(), "EE", Set.of("EUR", "USD"))
    );

    assertTrue(getTransactions(account.getId()).isEmpty());

    CreateTransactionResponse createTransactionResponse =
        createTransactionAndVerify(account,
            new CreateTransactionRequest(BigDecimal.TEN, "EUR", TransactionDirection.IN, "desc1"));
    assertEquals(BigDecimal.TEN, createTransactionResponse.currentBalance());

    account = getAccount(account.getId());

    assertEquals(BigDecimal.TEN, getBalance(account, "EUR").getBalance());
    assertEquals(BigDecimal.ZERO, getBalance(account, "USD").getBalance());

    CreateTransactionResponse createTransactionResponse2 =
        createTransactionAndVerify(account,
            new CreateTransactionRequest(BigDecimal.TEN, "EUR", TransactionDirection.OUT, "desc2"));
    assertEquals(BigDecimal.ZERO, createTransactionResponse2.currentBalance());

    account = getAccount(account.getId());

    assertEquals(BigDecimal.ZERO, getBalance(account, "EUR").getBalance());
    assertEquals(BigDecimal.ZERO, getBalance(account, "USD").getBalance());

    List<Transaction> transactions = getTransactions(account.getId());
    assertEquals(2, transactions.size());

    assertEquals(BigDecimal.TEN, transactions.get(0).getAmount());
    assertEquals("EUR", transactions.get(0).getCurrency());
    assertEquals("desc1", transactions.get(0).getDescription());
    assertEquals(TransactionDirection.IN, transactions.get(0).getDirection());
    assertEquals(account.getId(), transactions.get(0).getAccountId());

    assertEquals(BigDecimal.TEN, transactions.get(1).getAmount());
    assertEquals("EUR", transactions.get(1).getCurrency());
    assertEquals("desc2", transactions.get(1).getDescription());
    assertEquals(TransactionDirection.OUT, transactions.get(1).getDirection());
    assertEquals(account.getId(), transactions.get(1).getAccountId());
  }

  @Test
  void testInsufficientFunds() throws Exception {
    Account account = createAccountAndVerify(
        new CreateAccountRequest(UUID.randomUUID(), "EE", Set.of("EUR", "USD"))
    );
    CreateTransactionRequest transactionReq =
        new CreateTransactionRequest(BigDecimal.TEN, "EUR", TransactionDirection.OUT, "desc2");

    MvcResult result = mockMvc
        .perform(
            post("/api/account/{id}/transaction", account.getId())
                .content(mapper.writeValueAsBytes(transactionReq))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isBadRequest()).andReturn();

    ErrorResponse error =
        mapper.readValue(result.getResponse().getContentAsByteArray(), ErrorResponse.class);
    assertEquals("insufficient funds", error.error());
  }

  private List<Transaction> getTransactions(UUID id) throws Exception {
    MvcResult result = mockMvc
        .perform(get("/api/account/{id}/transactions", id))
        .andExpect(status().isOk()).andReturn();
    return mapper.readValue(result.getResponse().getContentAsByteArray(),
        TypeFactory.defaultInstance().constructCollectionType(List.class, Transaction.class));
  }

  private Account getAccount(UUID id) throws Exception {
    MvcResult result = mockMvc
        .perform(get("/api/account/{id}", id))
        .andExpect(status().isOk()).andReturn();
    return mapper.readValue(result.getResponse().getContentAsByteArray(), Account.class);
  }

  private AccountBalance getBalance(Account account, String currency) {
    return account.getAccountBalances().stream().filter(it -> it.getCurrency().equals(currency))
        .findFirst().orElseThrow();
  }

  private CreateTransactionResponse createTransactionAndVerify(Account account,
                                                               CreateTransactionRequest request)
      throws Exception {
    MvcResult result = mockMvc
        .perform(
            post("/api/account/{id}/transaction", account.getId())
                .content(mapper.writeValueAsBytes(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();
    CreateTransactionResponse createTransactionResponse =
        mapper.readValue(result.getResponse().getContentAsByteArray(),
            CreateTransactionResponse.class);

    assertNotNull(createTransactionResponse.transactionId());
    assertEquals(account.getId(), createTransactionResponse.accountId());
    assertEquals(request.amount(), createTransactionResponse.amount());
    assertEquals(request.currency(), createTransactionResponse.currency());
    assertEquals(request.direction(), createTransactionResponse.direction());
    assertEquals(request.description(), createTransactionResponse.description());

    TransactionCreateEvent tcEvent =
        mapper.readValue(readQueue().getBody(), TransactionCreateEvent.class);
    assertEquals(createTransactionResponse.transactionId(), tcEvent.transactionId());
    assertEquals(createTransactionResponse.accountId(), tcEvent.accountId());
    assertEquals(createTransactionResponse.amount(), tcEvent.amount());
    assertEquals(createTransactionResponse.currency(), tcEvent.currency());
    assertEquals(createTransactionResponse.direction(), tcEvent.direction());

    BalanceChangeEvent bcEvent = mapper.readValue(readQueue().getBody(), BalanceChangeEvent.class);
    assertEquals(createTransactionResponse.accountId(), bcEvent.accountId());
    assertEquals(createTransactionResponse.amount(), bcEvent.amount());
    assertEquals(createTransactionResponse.currentBalance(), bcEvent.newBalance());
    assertEquals(createTransactionResponse.currency(), bcEvent.currency());

    return createTransactionResponse;
  }

  private Account createAccountAndVerify(CreateAccountRequest accountRequest) throws Exception {
    MvcResult result = mockMvc
        .perform(
            post("/api/account/")
                .content(mapper.writeValueAsBytes(accountRequest))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

    Account account =
        mapper.readValue(result.getResponse().getContentAsByteArray(), Account.class);

    assertNotNull(account.getId());
    assertEquals(accountRequest.customerId(), account.getCustomerId());
    assertEquals(accountRequest.country(), account.getCountry());
    assertEquals(accountRequest.currencies().size(), account.getAccountBalances().size());
    for (AccountBalance balance : account.getAccountBalances()) {
      assertTrue(accountRequest.currencies().contains(balance.getCurrency()));
      assertEquals(BigDecimal.ZERO, balance.getBalance());
    }
    AccountCreateEvent event = mapper.readValue(readQueue().getBody(), AccountCreateEvent.class);
    assertEquals(account.getId(), event.accountId());
    return account;
  }

  private Message readQueue() {
    return template.receive(queue.getName(), 1000);
  }

  @AfterAll
  static void afterAll() {
    pgContainer.close();
    mqContainer.close();
  }
}
