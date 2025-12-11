package trade.tradestream.market.infra.jpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MarketPriceRepositoryTest {

    @Autowired
    private MarketPriceRepository marketPriceRepository;

    @Test
    @DisplayName("symbol 로 저장된 엔티티를 찾을 수 있다.")
    void findBySymbol_whenExists_returnEntity() {
        //given
        String symbol = "BTCUSDT";
        MarketPriceEntity saved = marketPriceRepository.save(
                new MarketPriceEntity(symbol, 68_000.0, Instant.now())
        );

        //when
        Optional<MarketPriceEntity> result = marketPriceRepository.findBySymbol(symbol);

        //then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
        assertThat(result.get().getSymbol()).isEqualTo(saved.getSymbol());
    }
}