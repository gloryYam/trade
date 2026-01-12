package trade.tradestream.market.infra;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import trade.tradestream.market.domain.PriceTick;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis 기반 최신 가격 저장소
 *
 * [의도]
 * - StringRedisTemplate 사용 → JSON 문자열로 저장
 * - ObjectMapper로 직렬화/역직렬화 → PriceTick <-> JSON
 * - TTL 60초 → 오래된 가격 자동 삭제 (WebSocket 끊기면 stale 데이터 방지)
 *
 * [Walking Skeleton]
 * - 에러 시 로그만 출력 → 예외 던지지 않음 (일단 동작 확인용)
 * - Step 2에서 예외 처리 추가 예정
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class LatestPriceRedisStore implements LatestPriceStore{

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "price:latest:";  // 키 패턴: price:latest:BTCUSDT
    private static final Duration TTL = Duration.ofSeconds(60);

    @Override
    public void save(PriceTick priceTick) {

        String key = KEY_PREFIX + priceTick.symbol();

        try {
            String value = objectMapper.writeValueAsString(priceTick);
            redisTemplate.opsForValue().set(key, value, TTL);

            log.info("priceTick value : {}", value);
            log.debug("가격 저장: {} = {}", priceTick.symbol(), priceTick.bidPrice());

        } catch (JsonProcessingException e) {

            log.error("가격 직렬화 실패: {}", priceTick.symbol(), e);

        }

    }

    @Override
    public Optional<PriceTick> findBySymbol(String symbol) {
        String key = KEY_PREFIX + symbol;
        String value = redisTemplate.opsForValue().get(key);

        if(value == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(value, PriceTick.class));

        } catch (JsonProcessingException e) {
            log.error("가격 역직렬화 실패: {}", symbol, e);
            return Optional.empty();
        }
    }
}
