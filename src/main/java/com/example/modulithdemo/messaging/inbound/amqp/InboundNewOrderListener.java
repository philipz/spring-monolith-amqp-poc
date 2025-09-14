package com.example.modulithdemo.messaging.inbound.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.example.modulithdemo.order.domain.Customer;
import com.example.modulithdemo.order.domain.OrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Inbound listener for RabbitMQ 'new-orders' queue.
 * Logs payload and publishes internal OrderCreatedEvent for downstream modules.
 */
@Component
public class InboundNewOrderListener {

  private static final Logger log = LoggerFactory.getLogger(InboundNewOrderListener.class);

  private final ApplicationEventPublisher events;
  private final ObjectMapper objectMapper;
  private final int maxAttempts;

  public InboundNewOrderListener(
      ApplicationEventPublisher events,
      ObjectMapper objectMapper,
      @Value("${app.amqp.new-orders.retry-max-attempts:3}") int maxAttempts
  ) {
    this.events = events;
    this.objectMapper = objectMapper;
    this.maxAttempts = maxAttempts;
  }

  // Listener with manual acknowledgments via a tuned container factory
  @RabbitListener(queues = AmqpConstants.NEW_ORDERS_QUEUE, containerFactory = "inboundListenerFactory")
  public void onMessage(Message message, Channel channel) {
    long tag = message.getMessageProperties().getDeliveryTag();
    String payload = new String(message.getBody(), StandardCharsets.UTF_8);
    log.info("[InboundNewOrderListener] received payload from 'new-orders': {}", payload);
    processWithAck(payload, () -> {
      try { channel.basicAck(tag, false); } catch (IOException e) { log.error("Ack failed", e); }
    }, () -> {
      try { channel.basicReject(tag, false); } catch (IOException e) { log.error("Reject failed", e); }
    });
  }

  // Delegate for unit tests to avoid needing Channel/Message
  public void handle(String payload) throws IOException {
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
    } catch (IOException e) {
      throw e; // propagate to trigger NACK in listener method
    } catch (Exception e) {
      throw new IOException("Failed to process message", e);
    }
  }

  // Package-private for tests
  void processWithAck(String payload, Runnable ack, Runnable reject) {
    int attempts = 0;
    while (true) {
      try {
        handle(payload);
        ack.run();
        return;
      } catch (Exception e) {
        attempts++;
        if (attempts >= maxAttempts) {
          log.warn("[InboundNewOrderListener] failing after {} attempts; dead-lettering", attempts, e);
          reject.run();
          return;
        } else {
          log.warn("[InboundNewOrderListener] attempt {}/{} failed; retrying", attempts, maxAttempts, e);
        }
      }
    }
  }
}
