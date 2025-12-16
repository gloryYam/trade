package trade.tradestream.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Getter
@ConfigurationProperties(prefix = "snapshot")
public class SnapshotPolicyProperties {

    private BigDecimal threshold;
}
