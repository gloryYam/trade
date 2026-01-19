package trade.tradestream.market.infra;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 *  Binance bookTicker WebSocket 메시지 DTO
 *
 *  {
 *      "s": "BTCUSDT",        // symbol - 거래쌍
 *      "b": "25.35190000",    // best bid price - 최고 매수 호가
 *      "a": "25.36520000",    // best ask price - 최저 매도 호가
 *  }
 */
public record BinanceBookTickerMessage(
        @JsonProperty("s") String symbol,           // "BTCUSDT"
        @JsonProperty("b") BigDecimal bidPrice,     // "매수 호가"
        @JsonProperty("a") BigDecimal askPrice      // "매도 호가"
) {

}
