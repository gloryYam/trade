package trade.tradestream.market.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import trade.tradestream.market.domain.PriceTick;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * LatestPriceRedisStore 단위 테스트
 *
 * [의도]
 * - Redis 실제 연결 없이 Mock으로 테스트
 * - save() 호출 시 Redis에 JSON 저장되는지 검증
 * - findBySymbol() 호출 시 JSON → PriceTick 변환 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LatestPriceRedisStore 테스트")
class LatestPriceRedisStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private LatestPriceRedisStore priceStore;

    @BeforeEach
    void setUp() {
        // ObjectMapper에 JavaTimeModule 등록 (Instant 직렬화용)
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        priceStore = new LatestPriceRedisStore(redisTemplate, objectMapper);
    }

    @Test
    @DisplayName("save() 호출 시 Redis에 JSON 저장")
    void save_Success() {
        // Given
        PriceTick priceTick = new PriceTick(
                "BTCUSDT",
                new BigDecimal("45000.00"),
                new BigDecimal("45010.00"),
                Instant.now()
        );
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // When
        priceStore.save(priceTick);

        // Then
        verify(valueOperations).set(
                eq("price:latest:BTCUSDT"),
                any(String.class),
                eq(Duration.ofSeconds(60))
        );
    }

    @Test
    @DisplayName("findBySymbol() 성공 시 PriceTick 반환")
    void findBySymbol_Success() throws Exception {
        // Given
        String symbol = "BTCUSDT";
        PriceTick expected = new PriceTick(
                symbol,
                new BigDecimal("45000.00"),
                new BigDecimal("45010.00"),
                Instant.now()
        );
        String json = objectMapper.writeValueAsString(expected);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("price:latest:" + symbol)).willReturn(json);

        // When
        Optional<PriceTick> result = priceStore.findBySymbol(symbol);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().symbol()).isEqualTo(symbol);
        assertThat(result.get().bidPrice()).isEqualByComparingTo(new BigDecimal("45000.00"));
    }

    @Test
    @DisplayName("findBySymbol() 데이터 없으면 Optional.empty 반환")
    void findBySymbol_NotFound() {
        // Given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("price:latest:UNKNOWN")).willReturn(null);

        // When
        Optional<PriceTick> result = priceStore.findBySymbol("UNKNOWN");

        // Then
        assertThat(result).isEmpty();
    }
}

