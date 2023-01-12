package pe.redis.sample.order.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class OrderRepository {

  @Value("classpath:json/order.json")
  Resource order;

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public List<Order> findListOrders() {
    try {
      return objectMapper.readValue(order.getFile(), new TypeReference<>() {});
    } catch (IOException e) {
      return Collections.emptyList();
    }
  }
}
