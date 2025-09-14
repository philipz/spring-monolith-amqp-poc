package com.example.modulithdemo.domain.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderCreatedEventListener.class);

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        logger.info("Processing OrderCreatedEvent orderNumber={}, productCode={}, quantity={}, customer={}",
                event.orderNumber(), event.productCode(), event.quantity(), event.customer());
        // TODO: Add business logic here (e.g., send notification, update inventory, etc.)
    }
}
