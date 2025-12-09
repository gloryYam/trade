package trade.tradestream.market.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import trade.tradestream.market.domain.MarketPriceEntity;

public interface MarketPriceRepository extends JpaRepository<MarketPriceEntity, Long> {

    MarketPriceEntity findBySymbol(String symbol);
}
