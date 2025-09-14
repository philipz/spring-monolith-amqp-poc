package com.example.modulithdemo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitListenerConfig {

  private static final Logger log = LoggerFactory.getLogger(RabbitListenerConfig.class);

  @Bean
  public SimpleRabbitListenerContainerFactory inboundListenerFactory(ConnectionFactory connectionFactory) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    // Performance tuning
    factory.setConcurrentConsumers(2);          // start with 2 consumers
    factory.setMaxConcurrentConsumers(8);       // scale up under load
    factory.setPrefetchCount(20);               // limit unacked messages per consumer
    // Reliability: manual acknowledgments
    factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
    factory.setDefaultRequeueRejected(false);   // avoid infinite redelivery loops
    log.info("Configured inboundListenerFactory: concurrency=2..8, prefetch=20, ack=MANUAL");
    return factory;
  }
}

