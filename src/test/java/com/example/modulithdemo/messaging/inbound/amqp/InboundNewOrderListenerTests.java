package com.example.modulithdemo.messaging.inbound.amqp;

import com.example.modulithdemo.order.domain.Customer;
import com.example.modulithdemo.order.domain.OrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InboundNewOrderListenerTests {

  static class CapturingPublisher implements ApplicationEventPublisher {
    final List<Object> events = new ArrayList<>();
    @Override public void publishEvent(Object event) { events.add(event); }
  }

  @Test
  void onMessage_publishesOrderCreatedEvent_onValidJson() throws Exception {
    CapturingPublisher publisher = new CapturingPublisher();
    ObjectMapper objectMapper = new ObjectMapper();
    InboundNewOrderListener listener = new InboundNewOrderListener(publisher, objectMapper, 3);

    String json = "{" +
        "\"orderNumber\":\"A123\"," +
        "\"productCode\":\"BOOK-001\"," +
        "\"quantity\":2," +
        "\"customer\":{\"name\":\"Alice\",\"email\":\"alice@example.com\",\"phone\":\"123\"}" +
        "}";

    assertDoesNotThrow(() -> listener.handle(json));

    assertEquals(1, publisher.events.size(), "should publish exactly one event");
    Object published = publisher.events.get(0);
    assertTrue(published instanceof OrderCreatedEvent);

    OrderCreatedEvent event = (OrderCreatedEvent) published;
    assertEquals("A123", event.orderNumber());
    assertEquals("BOOK-001", event.productCode());
    assertEquals(2, event.quantity());
    Customer c = event.customer();
    assertNotNull(c);
    assertEquals("Alice", c.name());
    assertEquals("alice@example.com", c.email());
    assertEquals("123", c.phone());
  }

  @Test
  void onMessage_throws_andDoesNotPublish_onMalformedJson() {
    CapturingPublisher publisher = new CapturingPublisher();
    ObjectMapper objectMapper = new ObjectMapper();
    InboundNewOrderListener listener = new InboundNewOrderListener(publisher, objectMapper, 3);

    String bad = "not-json";

    assertThrows(Exception.class, () -> listener.handle(bad));
    assertTrue(publisher.events.isEmpty(), "should not publish any events");
  }
}
