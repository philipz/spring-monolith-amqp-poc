package com.example.modulithdemo.messaging.inbound.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.*;

class InboundNewOrderListenerRetryTests {

  static class NoopPublisher implements ApplicationEventPublisher { public void publishEvent(Object event) { } }

  @Test
  void processWithAck_retries_then_acks_onSuccess() {
    class FlakyListener extends InboundNewOrderListener {
      int calls = 0;
      FlakyListener() { super(new NoopPublisher(), new ObjectMapper(), 3); }
      @Override public void handle(String payload) throws java.io.IOException {
        calls++;
        if (calls < 3) throw new java.io.IOException("boom"); // fail twice, then succeed
      }
    }

    FlakyListener listener = new FlakyListener();
    int[] ack = {0};
    int[] rej = {0};

    listener.processWithAck("irrelevant", () -> ack[0]++, () -> rej[0]++);

    assertEquals(1, ack[0], "should ack once after success");
    assertEquals(0, rej[0], "should not reject");
    assertEquals(3, listener.calls, "should attempt up to success");
  }

  @Test
  void processWithAck_retries_and_rejects_onExhaustion() {
    class AlwaysFail extends InboundNewOrderListener {
      int calls = 0;
      AlwaysFail() { super(new NoopPublisher(), new ObjectMapper(), 3); }
      @Override public void handle(String payload) throws java.io.IOException { calls++; throw new java.io.IOException("nope"); }
    }

    AlwaysFail listener = new AlwaysFail();
    int[] ack = {0};
    int[] rej = {0};

    listener.processWithAck("irrelevant", () -> ack[0]++, () -> rej[0]++);

    assertEquals(0, ack[0], "should not ack");
    assertEquals(1, rej[0], "should reject after retries");
    assertEquals(3, listener.calls, "should attempt max times");
  }
}
