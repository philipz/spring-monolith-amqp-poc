package com.example.modulithdemo.inventory;

import com.example.modulithdemo.DemoApplication;
import com.example.modulithdemo.order.domain.Customer;
import com.example.modulithdemo.order.domain.OrderCreatedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DemoApplication.class)
@Import(OrderCreatedFlowTests.TestListener.class)
class OrderCreatedFlowTests {

  @Autowired ApplicationEventPublisher events;

  @AfterEach
  void reset() {
    TestListener.reset();
  }

  @Test
  void publishesEvent_andListenerReceives() throws InterruptedException {
    OrderCreatedEvent event = new OrderCreatedEvent(
        "X-100",
        "BOOK-XYZ",
        3,
        new Customer("Bob", "bob@example.com", "987")
    );

    events.publishEvent(event);

    boolean received = TestListener.LATCH.await(2, TimeUnit.SECONDS);
    assertTrue(received, "Expected TestListener to receive OrderCreatedEvent");
    assertNotNull(TestListener.lastEvent, "Listener should capture last event");
    assertEquals("X-100", TestListener.lastEvent.orderNumber());
  }

  @Component
  static class TestListener {
    static final CountDownLatch LATCH = new CountDownLatch(1);
    static volatile OrderCreatedEvent lastEvent;

    static void reset() {
      lastEvent = null;
      while (LATCH.getCount() > 0) {
        // no-op; CountDownLatch can't be reset, but tests don't rely on count after reset
        break;
      }
    }

    @org.springframework.context.event.EventListener
    void on(OrderCreatedEvent event) {
      lastEvent = event;
      LATCH.countDown();
    }
  }
}
