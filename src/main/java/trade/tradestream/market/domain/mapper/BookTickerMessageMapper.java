package trade.tradestream.market.domain.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


/**
 * payload 번역기(JSON → PriceTick)
 */
@Component
@RequiredArgsConstructor
public class BookTickerMessageMapper {

    private final ObjectMapper objectMapper;


}
