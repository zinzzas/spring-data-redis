package pe.redis.properties;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties("redis")
public class RedisCacheProperties {

  private Cache cache;
  private CacheTtl cacheTtl;

  @Setter
  @Getter
  public static class Cache {
    private String env;
    private String nodes;
    private long commandTimeout;
    private long connectionTimeout;
  }

  @Setter
  @Getter
  public static class CacheTtl {
    private long defaultTtl;
    private Map<String, Long> cacheNamesTtl;
  }
}
