package pe.redis.sample.order.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.redis.sample.order.domain.Order;
import pe.redis.sample.order.reader.OrderReader;

@RequiredArgsConstructor
@Service
public class OrderService {
  private final OrderReader orderReader;

  public List<Order> getAllOrders() {
    return orderReader.getAllOrders()
                      .stream()
                      .collect(Collectors.toList());
  }
}
