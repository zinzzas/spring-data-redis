package pe.redis.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
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
    final ClientOptions clientOptions = ClientOptions.builder()
                                                     .socketOptions(socketOptions).build();
    LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                                                                               .clientOptions(clientOptions)
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

  public ObjectMapper objectMappers() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
    return mapper;
  }
}
