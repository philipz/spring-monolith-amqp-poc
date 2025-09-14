package com.example.modulithdemo.order.api;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.modulithdemo.order.app.OrderManagement;
import com.example.modulithdemo.order.domain.OrderCompletionException;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

  private static final Logger log = LoggerFactory.getLogger(OrderController.class);

  private final OrderManagement orders;

  @PostMapping("/{id}/complete")
  public ResponseEntity<String> complete(@PathVariable UUID id) {
    try {
      log.info("Received request to complete order: {}", id);

      orders.complete(id);

      String message = "Order " + id + " completed (event published)";
      log.info("Successfully completed order: {}", id);

      return ResponseEntity.accepted().body(message);

    } catch (OrderCompletionException e) {
      log.error("Failed to complete order {}: {}", id, e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to complete order " + id + ": " + e.getMessage());

    } catch (IllegalArgumentException e) {
      log.warn("Invalid order ID provided: {} - {}", id, e.getMessage());
      return ResponseEntity.badRequest()
          .body("Invalid order ID: " + e.getMessage());

    } catch (Exception e) {
      log.error("Unexpected error completing order {}: {}", id, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error occurred while completing order " + id);
    }
  }
}
