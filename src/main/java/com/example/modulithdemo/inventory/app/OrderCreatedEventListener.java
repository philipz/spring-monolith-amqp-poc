package com.example.modulithdemo.inventory.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.modulithdemo.order.domain.OrderCreatedEvent;

@Component
public class OrderCreatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventListener.class);

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Processing OrderCreatedEvent orderNumber={}, productCode={}, quantity={}, customer={}",
                event.orderNumber(), event.productCode(), event.quantity(), event.customer());
        // TODO: Add business logic here (e.g., send notification, update inventory, etc.)
    }
}
