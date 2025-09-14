package com.example.modulithdemo.messaging.inbound.amqp;

/**
 * Centralized AMQP configuration constants to avoid duplication.
 * Inbound-only variant: only includes inbound exchange/queue/routing for new orders.
 */
public final class AmqpConstants {

  private AmqpConstants() {
    // Prevent instantiation
  }

  // Inbound exchange used by external systems to publish new orders (optional bind)
  public static final String BOOKSTORE_EXCHANGE = "BookStoreExchange";

  // Inbound queue our application listens to
  public static final String NEW_ORDERS_QUEUE = "new-orders";

  // Routing Keys for inbound messages
  public static final String ORDERS_NEW_ROUTING = "orders.new";
}
