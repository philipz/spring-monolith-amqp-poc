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
    return QueueBuilder.durable(NEW_ORDERS_QUEUE)
        .withArgument("x-dead-letter-exchange", BOOKSTORE_DLX)
        .withArgument("x-dead-letter-routing-key", ORDERS_NEW_DLQ_ROUTING)
        .build();
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

  // Dead-letter exchange and queue are unconditional; they don't create connections until used
  @Bean
  DirectExchange newOrderDlx() {
    return new DirectExchange(BOOKSTORE_DLX, true, false);
  }

  @Bean
  Queue newOrderDlq() {
    return QueueBuilder.durable(NEW_ORDERS_DLQ).build();
  }

  @Bean
  Binding bindNewOrderDlq(Queue newOrderDlq, DirectExchange newOrderDlx) {
    return BindingBuilder.bind(newOrderDlq).to(newOrderDlx).with(ORDERS_NEW_DLQ_ROUTING);
  }
}
