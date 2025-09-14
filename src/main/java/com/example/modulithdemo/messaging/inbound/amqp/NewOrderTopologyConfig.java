package com.example.modulithdemo.messaging.inbound.amqp;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.modulithdemo.messaging.inbound.amqp.AmqpConstants.*;

@Configuration
public class NewOrderTopologyConfig {

  @Bean
  Queue newOrderQueue() {
    return QueueBuilder.durable(NEW_ORDERS_QUEUE).build();
  }

  @Bean
  @ConditionalOnProperty(name = "app.amqp.new-orders.bind", havingValue = "true")
  DirectExchange newOrderExchange() {
    return new DirectExchange(BOOKSTORE_EXCHANGE, true, false);
  }

  @Bean
  @ConditionalOnProperty(name = "app.amqp.new-orders.bind", havingValue = "true")
  Binding bindNewOrderQueue(Queue newOrderQueue, DirectExchange newOrderExchange) {
    return BindingBuilder.bind(newOrderQueue).to(newOrderExchange).with(ORDERS_NEW_ROUTING);
  }
}
