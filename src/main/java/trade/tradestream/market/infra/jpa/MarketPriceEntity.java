package trade.tradestream.market.infra.jpa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@Table(name = "market_price")
@NoArgsConstructor
public class MarketPriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String symbol;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private Instant updatedAt;

    public MarketPriceEntity(String symbol, double price, Instant updatedAt) {
        this.symbol = symbol;
        this.price = price;
        this.updatedAt = updatedAt;
    }
}
