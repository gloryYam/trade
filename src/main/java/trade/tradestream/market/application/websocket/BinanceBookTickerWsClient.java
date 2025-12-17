package trade.tradestream.market.application.websocket;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import trade.tradestream.config.BinanceWsProperties;
import trade.tradestream.market.domain.mapper.BookTickerMessageMapper;
import trade.tradestream.market.infra.redis.LatestPriceRedisStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 연결하고 메시지 받는 곳
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(BinanceWsProperties.class)
public class BinanceBookTickerWsClient {

    private final LatestPriceRedisStore latestPriceRedisStore;
    private final BookTickerMessageMapper bookTickerMessageMapper;
    private final BinanceWsProperties binanceWsProperties;

    private final StandardWebSocketClient client = new StandardWebSocketClient();
    private final Map<String, WebSocketClient> sesstion = new ConcurrentHashMap<>();

    public void start() {
        for(String raw : binanceWsProperties.getSymbols()) {

        }
    }
}
