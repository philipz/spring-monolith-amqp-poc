package com.example.modulithdemo.inbound.amqp;

import com.example.modulithdemo.domain.order.Customer;
import com.example.modulithdemo.domain.order.OrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InboundNewOrderListenerTests {

  @Test
  void onMessage_publishesOrderCreatedEvent_onValidJson() throws Exception {
    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    ObjectMapper objectMapper = new ObjectMapper();
    InboundNewOrderListener listener = new InboundNewOrderListener(publisher, objectMapper);

    String json = "{" +
        "\"orderNumber\":\"A123\"," +
        "\"productCode\":\"BOOK-001\"," +
        "\"quantity\":2," +
        "\"customer\":{\"name\":\"Alice\",\"email\":\"alice@example.com\",\"phone\":\"123\"}" +
        "}";

    assertDoesNotThrow(() -> listener.onMessage(json));

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(publisher, times(1)).publishEvent(captor.capture());
    Object published = captor.getValue();
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
  void onMessage_doesNotThrow_orPublish_onMalformedJson() {
    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    ObjectMapper objectMapper = new ObjectMapper();
    InboundNewOrderListener listener = new InboundNewOrderListener(publisher, objectMapper);

    String bad = "not-json";

    assertDoesNotThrow(() -> listener.onMessage(bad));
    verify(publisher, never()).publishEvent(any());
  }
}

