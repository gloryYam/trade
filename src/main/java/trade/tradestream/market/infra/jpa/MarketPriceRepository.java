package trade.tradestream.market.infra.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarketPriceRepository extends JpaRepository<MarketPriceEntity, Long> {

    Optional<MarketPriceEntity> findBySymbol(String symbol);
}
