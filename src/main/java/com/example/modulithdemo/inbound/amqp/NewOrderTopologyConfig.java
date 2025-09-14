package com.example.modulithdemo.inbound.amqp;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("amqp")
public class NewOrderTopologyConfig {

  public static final String QUEUE = "new-orders";
  public static final String EXCHANGE = "BookStoreExchange";
  public static final String ROUTING = "orders.new";

  @Bean
  Queue newOrderQueue() {
    return QueueBuilder.durable(QUEUE).build();
  }

  @Bean
  @ConditionalOnProperty(name = "app.amqp.new-orders.bind", havingValue = "true")
  DirectExchange newOrderExchange() {
    return new DirectExchange(EXCHANGE, true, false);
  }

  @Bean
  @ConditionalOnProperty(name = "app.amqp.new-orders.bind", havingValue = "true")
  Binding bindNewOrderQueue(Queue newOrderQueue, DirectExchange newOrderExchange) {
    return BindingBuilder.bind(newOrderQueue).to(newOrderExchange).with(ROUTING);
  }
}
