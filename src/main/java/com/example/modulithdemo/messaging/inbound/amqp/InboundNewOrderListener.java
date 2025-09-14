package com.example.modulithdemo.messaging.inbound.amqp;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.example.modulithdemo.order.domain.Customer;
import com.example.modulithdemo.order.domain.OrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Inbound listener for RabbitMQ 'new-orders' queue.
 * Logs payload and publishes internal OrderCreatedEvent for downstream modules.
 */
@Component
@RequiredArgsConstructor
public class InboundNewOrderListener {

  private static final Logger log = LoggerFactory.getLogger(InboundNewOrderListener.class);

  private final ApplicationEventPublisher events;
  private final ObjectMapper objectMapper;

  @RabbitListener(queues = AmqpConstants.NEW_ORDERS_QUEUE)
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
