package trade.tradestream.market.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import trade.tradestream.config.SnapshotPolicyProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class SnapshotUpdatePolicy {

    private final SnapshotPolicyProperties properties;

    public boolean shouldUpdate(BigDecimal lastSnapshotPrice, BigDecimal newPrice) {
        if(lastSnapshotPrice == null || lastSnapshotPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }

        BigDecimal chageRate = newPrice
                .subtract(lastSnapshotPrice)
                .divide(lastSnapshotPrice, 6, RoundingMode.HALF_UP)
                .abs();

        return chageRate.compareTo(properties.getThreshold()) >= 0;
    }
}
