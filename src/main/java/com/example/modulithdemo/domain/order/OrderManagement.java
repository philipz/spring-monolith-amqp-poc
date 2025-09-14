package com.example.modulithdemo.domain.order;

import java.util.UUID;
import java.util.logging.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
public class OrderManagement {

  private static final Logger logger = Logger.getLogger(OrderManagement.class.getName());

  private final ApplicationEventPublisher events;

  public OrderManagement(ApplicationEventPublisher events) {
    this.events = events;
  }

  @Transactional
  public void complete(UUID orderId) {
    Assert.notNull(orderId, "Order ID must not be null");

    logger.info("Completing order with ID: " + orderId);

    // TODO: Add actual order validation and status update logic here
    // For now, we'll just validate the ID and publish the event

    try {
      events.publishEvent(new OrderCompleted(orderId));
      logger.info("Order completion event published for order ID: " + orderId);
    } catch (Exception e) {
      logger.severe("Failed to publish order completion event for order ID: " + orderId + " - " + e.getMessage());
      throw new OrderCompletionException("Failed to complete order: " + orderId, e);
    }
  }
}
