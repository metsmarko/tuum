package ee.metsmarko.tuum.account;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class RabbitMqTestConf {
  @Bean
  public Queue queue() {
    return new Queue("test-queue");
  }

  @Bean
  public Binding binding(TopicExchange topic, Queue queue) {
    return BindingBuilder.bind(queue)
        .to(topic)
        .with("account.*.#");
  }
}
