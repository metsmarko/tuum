package ee.metsmarko.tuum.conf;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConf {
  @Bean
  public TopicExchange exchange(@Value("${tuum.account.mq.exchange-name}") String exchangeName) {
    return new TopicExchange(exchangeName);
  }
}
