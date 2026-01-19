package trade.tradestream.config;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson ObjectMapper 설정
 *
 * ============================================================
 * [왜 필요한가?]
 * ============================================================
 * Spring Boot가 기본 ObjectMapper를 제공하지만,
 * 명시적으로 설정하면:
 * 1. 동작을 예측 가능하게 만듦
 * 2. 프로젝트 전체에서 일관된 JSON 처리
 * 3. 특수 타입 (Instant, BigDecimal) 처리 보장
 *
 * ============================================================
 * [주요 설정 설명]
 * ============================================================
 *
 * JavaTimeModule:
 *   Java 8 날짜/시간 타입 (Instant, LocalDateTime 등) 직렬화 지원
 *   이거 없으면 Instant를 JSON으로 변환할 때 에러 발생
 *
 * FAIL_ON_UNKNOWN_PROPERTIES = false:
 *   JSON에 Java 클래스에 없는 필드가 있어도 에러 안 남
 *   Binance가 새 필드 추가해도 우리 코드 안 깨짐
 *   (예: Binance가 "newField": 123 추가해도 무시하고 파싱)
 */

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Java 8 날짜/시간 타입 지원 (Instant 등)
        mapper.registerModule(new JavaTimeModule());

        // 알 수 없는 필드 무시 (Binance API 변경에 유연하게 대응)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return mapper;
    }

}
