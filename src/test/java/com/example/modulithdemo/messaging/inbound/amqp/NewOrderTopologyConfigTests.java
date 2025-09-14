package com.example.modulithdemo.messaging.inbound.amqp;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;

import java.util.Map;

import static com.example.modulithdemo.messaging.inbound.amqp.AmqpConstants.*;
import static org.junit.jupiter.api.Assertions.*;

class NewOrderTopologyConfigTests {

  @Test
  void newOrderQueue_hasDlqArguments_andDlqBeansPresent() {
    try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(ctx, "app.amqp.new-orders.bind=true");
      ctx.register(NewOrderTopologyConfig.class);
      ctx.refresh();

      Queue q = ctx.getBean("newOrderQueue", Queue.class);
      assertEquals(NEW_ORDERS_QUEUE, q.getName());
      Map<String, Object> args = q.getArguments();
      assertEquals(BOOKSTORE_DLX, args.get("x-dead-letter-exchange"));
      assertEquals(ORDERS_NEW_DLQ_ROUTING, args.get("x-dead-letter-routing-key"));

      Queue dlq = ctx.getBean("newOrderDlq", Queue.class);
      assertEquals(NEW_ORDERS_DLQ, dlq.getName());

      DirectExchange dlx = ctx.getBean("newOrderDlx", DirectExchange.class);
      assertEquals(BOOKSTORE_DLX, dlx.getName());
    }
  }
}
