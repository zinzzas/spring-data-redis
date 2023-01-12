package pe.redis.sample.order.reader;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import pe.redis.sample.order.domain.Order;
import pe.redis.sample.order.domain.OrderRepository;

@RequiredArgsConstructor
@Component
public class OrderReader {
  private final OrderRepository orderRepository;

  @Cacheable(cacheManager = "redisCacheManager", cacheNames = "getAllOrder", unless = "#result == null")
  public List<Order> getAllOrders() {
    return orderRepository.findListOrders();
  }
}
