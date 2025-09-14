package com.example.modulithdemo.order.app;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import com.example.modulithdemo.order.domain.OrderCompleted;
import com.example.modulithdemo.order.domain.OrderCompletionException;

@Service
@RequiredArgsConstructor
public class OrderManagement {

  private static final Logger log = LoggerFactory.getLogger(OrderManagement.class);

  private final ApplicationEventPublisher events;

  @Transactional
  public void complete(UUID orderId) {
    Assert.notNull(orderId, "Order ID must not be null");

    log.info("Completing order with ID: {}", orderId);

    // TODO: Add actual order validation and status update logic here
    // For now, we'll just validate the ID and publish the event

    try {
      events.publishEvent(new OrderCompleted(orderId));
      log.info("Order completion event published for order ID: {}", orderId);
    } catch (Exception e) {
      log.error("Failed to publish order completion event for order ID: {} - {}", orderId, e.getMessage(), e);
      throw new OrderCompletionException("Failed to complete order: " + orderId, e);
    }
  }
}
