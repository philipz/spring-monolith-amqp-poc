package com.example.modulithdemo.inventory.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.example.modulithdemo.order.domain.OrderCompleted;

@Component
public class InventoryManagement {
  private static final Logger log = LoggerFactory.getLogger(InventoryManagement.class);

  @ApplicationModuleListener
  void on(OrderCompleted event) {
    log.info("[Inventory] received OrderCompleted: {}", event);
  }
}
