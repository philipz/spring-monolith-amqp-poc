package com.example.modulithdemo.inbound.amqp;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.example.modulithdemo.domain.order.OrderCompleted;

@Component
public class InboundAmqpAdapter {
  private static final Logger log = LoggerFactory.getLogger(InboundAmqpAdapter.class);

  private final ApplicationEventPublisher events;

  public InboundAmqpAdapter(ApplicationEventPublisher events) {
    this.events = events;
  }

  @RabbitListener(queues = RabbitTopologyConfig.QUEUE)
  public void onMessage(String payload) {
    log.info("[InboundAmqpAdapter] received payload from queue: {}", payload);
    try {
      UUID id = UUID.fromString(payload.trim());
      events.publishEvent(new OrderCompleted(id));
    } catch (IllegalArgumentException e) {
      log.warn("Payload is not a UUID: {}", payload);
    }
  }
}