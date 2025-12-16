package trade.tradestream.market.infra.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import trade.tradestream.market.domain.PriceTick;
import trade.tradestream.market.infra.config.redis.LatestPriceRedisProperties;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LatestPriceRedisStore {

    private final RedisTemplate<String, PriceTick> redisTemplate;
    private final LatestPriceRedisProperties properties;

    public void put(PriceTick tick) {
        String symbol = normalizeSymbol(tick.symbol());
        if (symbol.isEmpty()) {
            log.warn("Skip caching: empty symbol");
            return;
        }

        String key = keyOf(symbol);
        try {
            redisTemplate.opsForValue().set(key, tick, properties.getTtl());
        } catch (Exception e) {
            log.warn("Failed to cache latest price. key={}", key, e);
        }
    }

    public Optional<PriceTick> get(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized.isEmpty()) return Optional.empty();

        String key = keyOf(normalized);

        try {
            return Optional.ofNullable((redisTemplate.opsForValue().get(key));
        } catch (Exception e) {
            log.warn("Failed to read latest price cache. key={}", key, e);
            return Optional.empty();
        }
    }

    private String keyOf(String normalizedSymbol) {
        return properties.getKeyPrefix() + normalizedSymbol;
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null) return "";
        String s = symbol.trim();
        return s.isEmpty() ? "" : s.toUpperCase();
    }
 