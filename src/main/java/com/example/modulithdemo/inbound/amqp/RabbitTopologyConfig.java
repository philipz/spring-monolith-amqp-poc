package com.example.modulithdemo.inbound.amqp;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {
  public static final String EXCHANGE = "domain.events";
  public static final String QUEUE = "new-orders";
  public static final String ROUTING = "order.completed";

  @Bean
  DirectExchange domainEventsExchange() {
    return new DirectExchange(EXCHANGE, true, false);
  }

  @Bean
  Queue inventoryQueue() {
    return QueueBuilder.durable(QUEUE).build();
  }

  @Bean
  Binding bindInventoryQueue(Queue inventoryQueue, DirectExchange domainEventsExchange) {
    return BindingBuilder.bind(inventoryQueue).to(domainEventsExchange).with(ROUTING);
  }
}