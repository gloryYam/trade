package trade.tradestream.market.api;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trade.tradestream.common.dto.ApiResponse;
import trade.tradestream.market.application.MarketService;
import trade.tradestream.market.domain.PriceTick;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;

    /**
     * 심볼의 최신 가격 조회
     * GET /api/market/prices/{symbol}
     */
    @GetMapping("/prices/{symbol}")
    public ApiResponse<PriceResponse> getLatestPrice(@PathVariable  String symbol) {
        PriceTick priceTick = marketService.getLatestPrice(symbol);
        return ApiResponse.success(PriceResponse.from(priceTick));
    }
}
