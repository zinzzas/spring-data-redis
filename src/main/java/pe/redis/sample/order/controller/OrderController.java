package pe.redis.sample.order.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import pe.redis.sample.order.service.OrderService;

@RequiredArgsConstructor
@Controller
public class OrderController {
  private final OrderService orderService;

  @GetMapping("/order/allOrders")
  public ResponseEntity getAllOrders() {
    return new ResponseEntity(orderService.getAllOrders(), HttpStatus.OK);
  }
}
