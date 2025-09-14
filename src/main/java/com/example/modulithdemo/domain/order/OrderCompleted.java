package com.example.modulithdemo.domain.order;

import java.util.UUID;

import org.springframework.modulith.events.Externalized;

@Externalized("domain.events::order.completed")
public record OrderCompleted(UUID orderId) {}
