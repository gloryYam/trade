package trade.tradestream.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@ConfigurationProperties(prefix = "binance.ws")
public class BinanceWsProperties {

    private String baseUrl = "wss://stream.binance.com:9443/ws";
    private List<String> symbols = List.of("BTCUSDT", "ETHUSDT", "ETHBTCUSDT");
}
