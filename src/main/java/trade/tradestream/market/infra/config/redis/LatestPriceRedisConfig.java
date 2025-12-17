package trade.tradestream.market.infra.config.redis;

import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import trade.tradestream.market.domain.PriceTick;

@Configuration
public class LatestPriceRedisConfig {

    @Bean
    public RedisTemplate<String, PriceTick> redisTemplate(RedisConnectionFactory factory, ObjectMapper objectMapper) {

        var template = new RedisTemplate<String, PriceTick>();
        template.setConnectionFactory(factory);

        var keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<PriceTick> valueSerializer = new Jackson2JsonRedisSerializer<>(PriceTick.class);
        valueSerializer.setObjectMapper(objectMapper);

        template.setKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.afterPropertiesSet();

        return template;
    }
}
