package ee.metsmarko.tuum.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;

class RabbitMqConfTest {
  @Test
  void testCreateExchange() {
    TopicExchange exchange = new RabbitMqConf().exchange("my-exchange");

    assertEquals("my-exchange", exchange.getName());
  }
}
