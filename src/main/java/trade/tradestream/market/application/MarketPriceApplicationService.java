package trade.tradestream.market.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import trade.tradestream.market.domain.PriceSnapshot;
import trade.tradestream.market.infra.jpa.MarketPriceEntity;
import trade.tradestream.market.infra.jpa.MarketPriceRepository;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MarketPriceApplicationService {

    private final MarketPriceRepository marketPriceRepository;

    private static final double DEFAULT_SAMPLE_PRICE = 68_000.0;

    public PriceSnapshot getOrCreateSamplePrice(PriceRequestService request) {

        var getSymbolData = request.getSymbol();

        MarketPriceEntity entity = marketPriceRepository.findBySymbol(getSymbolData)
                .orElseGet(() -> createSamplePrice(getSymbolData));

        return PriceSnapshot.from(entity);
    }



    private MarketPriceEntity createSamplePrice(String symbol) {

        MarketPriceEntity created = new MarketPriceEntity(
                symbol,
                DEFAULT_SAMPLE_PRICE,
                Instant.now()
        );

        return marketPriceRepository.save(created);
    }
}
