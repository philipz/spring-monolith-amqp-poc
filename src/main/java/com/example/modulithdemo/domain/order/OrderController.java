package com.example.modulithdemo.domain.order;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

  private final OrderManagement orders;

  @PostMapping("/{id}/complete")
  public ResponseEntity<String> complete(@PathVariable("id") UUID id) {
    orders.complete(id);
    return ResponseEntity.accepted().body("Order " + id + " completed (event published)");
  }
}