package trade.tradestream.market.infra.config.redis;


import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@ConfigurationProperties
public class LatestPriceRedisProperties {

    private String keyPrefix = "price:";
    private Duration ttl = Duration.ofSeconds(10);
}
