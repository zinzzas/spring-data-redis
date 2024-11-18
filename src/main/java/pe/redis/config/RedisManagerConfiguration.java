package pe.redis.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import pe.redis.properties.RedisCacheProperties;

@Slf4j
@EnableCaching
@Configuration
public class RedisManagerConfiguration {
  
  @Bean(name = "redisConnectionFactory")
  public LettuceConnectionFactory redisConnectionFactory(RedisCacheProperties redisCacheProperties) {

    final SocketOptions socketOptions = SocketOptions.builder()
                                                     .connectTimeout(Duration.ofSeconds(redisCacheProperties.getCache().getConnectionTimeout())).build();

    final ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()
        .topologyRefreshOptions(getClusterClientOptions())
        .socketOptions(socketOptions)
        .validateClusterNodeMembership(false)
        .build();

    LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                                                                               .clientOptions(clusterClientOptions)
                                                                               .commandTimeout(Duration.ofSeconds(redisCacheProperties.getCache().getConnectionTimeout())).build();

    RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration(Collections.singleton(redisCacheProperties.getCache().getNodes()));
    LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(redisClusterConfiguration, clientConfiguration);
    lettuceConnectionFactory.setValidateConnection(false);

    return lettuceConnectionFactory;
  }

  @Bean(name = "redisCacheManager")
  public CacheManager redisCacheManager(@Qualifier("redisConnectionFactory") LettuceConnectionFactory redisConnectionFactory, RedisCacheProperties redisCacheProperties) {
    RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(redisConnectionFactory);
    builder.cacheDefaults(redisCacheConfiguration(redisCacheProperties))
           .withInitialCacheConfigurations(redisCacheConfigurationMap(redisCacheProperties));
    return builder.build();
  }

  private RedisCacheConfiguration redisCacheConfiguration(RedisCacheProperties redisCacheProperties) {
    return RedisCacheConfiguration.defaultCacheConfig()
                                  .disableCachingNullValues()
                                  .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                                  //.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                                  //.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                                  .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMappers())))
                                  .prefixCacheNameWith(redisCacheProperties.getCache().getEnv() + ":boot:")
                                  .entryTtl(Duration.ofSeconds(redisCacheProperties.getCacheTtl().getDefaultTtl()));
  }

  // properties ttl
  private Map<String, RedisCacheConfiguration> redisCacheConfigurationMap(RedisCacheProperties redisCacheProperties) {
    Map<String, RedisCacheConfiguration> configurationMap = new HashMap<>();
    redisCacheProperties.getCacheTtl().getCacheNamesTtl().entrySet()
                        .forEach(o -> configurationMap.put(o.getKey(), redisCacheConfiguration(redisCacheProperties).entryTtl(Duration.ofSeconds(o.getValue()))));
    return configurationMap;
  }

  private ObjectMapper objectMapper() {
    PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                                                                .allowIfSubType("pe.redis")  // domainObject row type deserialization 허용.
                                                                .allowIfSubType(List.class)
                                                                .allowIfSubType(Object.class)
                                                                .allowIfBaseType(ArrayList.class)
                                                                .build();
    return Jackson2ObjectMapperBuilder.json()
                                      .failOnUnknownProperties(false) // unknown json filed ignore
                                      .build()
                                      .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL);
  }

  private ObjectMapper objectMappers() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
    return mapper;
  }

  /**
   * enablePeriodicRefresh(): 지정한 시간마다 클러스터 구성 정보를 가져와서 업데이트한다. 노드가 추가/삭제되었거나, 노드 다운으로 역할 변경(마스터 -> 복제(replica)),
   *  슬롯이 이동했을 경우 등 클러스터 구성 정보가 변경되었을 경우 최신 정보로 업데이트해야 한다.
   *  노드가 많을 경우 너무 짧은 시간을 지정하면 refresh 부하가 발생할 수 있다.
   * enableAllAdaptiveRefreshTriggers : 문제가 되는 Operation 발생시 커넥션을 갱신시켜주는 트리거 발생시킴
   * setReadFrom(ReadFrom.SLAVE): 이렇게 설정하면 get 같은 조회 명령은 복제(replica) 노드에서 실행된다.
   * 마스터/복제(replica)간 부하 분산(Load-Balancing)을 할 수 있다.
   *
   * @return
   */
  private ClusterTopologyRefreshOptions getClusterClientOptions() {
        return ClusterTopologyRefreshOptions.builder()
                .dynamicRefreshSources(true)                                  // default: true
                .enablePeriodicRefresh(Duration.ofSeconds(60))
                .enableAllAdaptiveRefreshTriggers()
                .adaptiveRefreshTriggersTimeout(Duration.ofSeconds(30))       // default: 30초
                .build();
    }
}
