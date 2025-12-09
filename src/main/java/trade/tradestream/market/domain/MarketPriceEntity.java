package trade.tradestream.market.domain;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
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
}
