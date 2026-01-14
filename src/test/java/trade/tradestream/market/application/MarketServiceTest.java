package trade.tradestream.market.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trade.tradestream.market.domain.PriceNotFoundException;
import trade.tradestream.market.domain.PriceTick;
import trade.tradestream.market.infra.LatestPriceStore;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketServiceTest {

    @Mock
    private LatestPriceStore latestPriceStore;

    @InjectMocks
    private MarketService marketService;

    @Test
    @DisplayName("가격 조회 성공 - 캐시에 데이터가 있을 때")
    void getLatestPrice_Success() {
        // Given
        String symbol = "BTCUSDT";
        PriceTick priceTick = new PriceTick(
                symbol,
                new BigDecimal("50000.00"),
                new BigDecimal("50001.00"),
                Instant.now()
        );
        when(latestPriceStore.findBySymbol(symbol)).thenReturn(Optional.of(priceTick));

        // When
        PriceTick result = marketService.getLatestPrice(symbol);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.symbol()).isEqualTo(symbol);
        assertThat(result.bidPrice()).isEqualTo(new BigDecimal("50000.00"));
    }

    @Test
    @DisplayName("가격 조회 성공 - 소문자 심볼도 대문자로 변환")
    void getLatestPrice_LowerCaseSymbol() {
        // Given
        String inputSymbol = "btcusdt";
        String upperSymbol = "BTCUSDT";
        PriceTick priceTick = new PriceTick(
                upperSymbol,
                new BigDecimal("50000.00"),
                new BigDecimal("50001.00"),
                Instant.now()
        );
        when(latestPriceStore.findBySymbol(upperSymbol)).thenReturn(Optional.of(priceTick));

        // When
        PriceTick result = marketService.getLatestPrice(inputSymbol);

        // Then
        assertThat(result.symbol()).isEqualTo(upperSymbol);
    }

    @Test
    @DisplayName("가격 조회 실패 - 캐시에 데이터가 없을 때 예외 발생")
    void getLatestPrice_NotFound() {
        // Given
        String symbol = "UNKNOWN";
        when(latestPriceStore.findBySymbol(symbol)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> marketService.getLatestPrice(symbol))
                .isInstanceOf(PriceNotFoundException.class)
                .hasMessageContaining(symbol);
    }

    @Test
    @DisplayName("가격 저장 - Store에 위임")
    void savePrice_DelegatesToStore() {
        // Given
        PriceTick priceTick = new PriceTick(
                "ETHUSDT",
                new BigDecimal("3000.00"),
                new BigDecimal("3001.00"),
                Instant.now()
        );

        // When
        marketService.savePrice(priceTick);

        // Then
        verify(latestPriceStore, times(1)).save(priceTick);
    }
}
