package com.example.modulithdemo.inbound.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import com.example.modulithdemo.domain.order.Customer;
import com.example.modulithdemo.domain.order.OrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Inbound listener for RabbitMQ 'new-orders' queue.
 * Logs payload and publishes internal OrderCreatedEvent for downstream modules.
 */
@Component
@Profile("amqp")
public class InboundNewOrderListener {
  private static final Logger log = LoggerFactory.getLogger(InboundNewOrderListener.class);

  private final ApplicationEventPublisher events;
  private final ObjectMapper objectMapper;

  public InboundNewOrderListener(ApplicationEventPublisher events, ObjectMapper objectMapper) {
    this.events = events;
    this.objectMapper = objectMapper;
  }

  @RabbitListener(queues = "new-orders")
  public void onMessage(String payload) {
    log.info("[InboundNewOrderListener] received payload from 'new-orders': {}", payload);
    try {
      NewOrderMessage dto = objectMapper.readValue(payload, NewOrderMessage.class);
      NewOrderMessage.NewOrderCustomer c = dto.customer();
      Customer customer = new Customer(
        c != null ? c.name() : null,
        c != null ? c.email() : null,
        c != null ? c.phone() : null
      );
      OrderCreatedEvent event = new OrderCreatedEvent(
        dto.orderNumber(),
        dto.productCode(),
        dto.quantity(),
        customer
      );
      events.publishEvent(event);
      log.info("[InboundNewOrderListener] published OrderCreatedEvent: {}", event);
    } catch (Exception e) {
      log.error("[InboundNewOrderListener] failed to process payload: {}", payload, e);
    }
  }
}
