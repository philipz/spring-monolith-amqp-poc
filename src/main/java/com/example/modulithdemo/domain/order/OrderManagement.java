package com.example.modulithdemo.domain.order;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderManagement {

  private final ApplicationEventPublisher events;

  @Transactional
  public void complete(UUID orderId) {
    events.publishEvent(new OrderCompleted(orderId));
  }
}
}
