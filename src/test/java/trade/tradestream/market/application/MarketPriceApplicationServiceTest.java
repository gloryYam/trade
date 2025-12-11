package trade.tradestream.market.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trade.tradestream.market.domain.PriceSnapshot;
import trade.tradestream.market.infra.jpa.MarketPriceEntity;
import trade.tradestream.market.infra.jpa.MarketPriceRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MarketPriceApplicationServiceTest {

    @Mock
    private MarketPriceRepository marketPriceRepository;

    @InjectMocks
    private MarketPriceApplicationService marketPriceApplicationService;

    @Test
    @DisplayName("심볼이 이미 존재하면 새로 생성하지 않고 기존 엔티티로 스냅샷을 반환한다")
    void getOrCreatePrice_whenSymbolExists_returnsExisting() {

        // given
        String symbol = "BTCUSDT";
        MarketPriceEntity entity = new MarketPriceEntity(symbol, 70_000.0, Instant.now());
        given(marketPriceRepository.findBySymbol(symbol))
                .willReturn(Optional.of(entity));

        // PriceRequestService 생성 방식은 실제 코드에 맞게 수정해줘
        PriceRequestService request = new PriceRequestService(symbol);

        // when
        PriceSnapshot snapshot = marketPriceApplicationService.getOrCreateSamplePrice(request);

        //than
        assertThat(snapshot.getSymbol()).isEqualTo(symbol);
        assertThat(snapshot.getPrice()).isEqualTo(70_000.0);
    }

    @Test
    @DisplayName("심볼이 없으면 새로 생성하여 저장하고 스냅샷을 반환한다")
    void getOrCreateSamplePrice_whenSymbolNotExists_createsNew() {
        // given
        String symbol = "BTCUSDT";
        given(marketPriceRepository.findBySymbol(symbol))
                .willReturn(Optional.empty());

        // createSamplePrice에서 만든 엔티티가 save 될 때 리턴된다고 가정
        MarketPriceEntity saved = new MarketPriceEntity(symbol, 68_000.0, Instant.now());
        given(marketPriceRepository.save(any(MarketPriceEntity.class)))
                .willReturn(saved);

        PriceRequestService request = new PriceRequestService(symbol);

        // when
        PriceSnapshot snapshot = marketPriceApplicationService.getOrCreateSamplePrice(request);

        // then
        assertThat(snapshot.getSymbol()).isEqualTo(symbol);
        assertThat(snapshot.getPrice()).isEqualTo(68_000.0);
    }
}