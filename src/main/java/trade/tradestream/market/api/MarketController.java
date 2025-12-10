package trade.tradestream.market.api;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trade.tradestream.common.dto.ApiResponse;
import trade.tradestream.market.application.MarketPriceApplicationService;
import trade.tradestream.market.application.PriceRequestService;
import trade.tradestream.market.domain.PriceSnapshot;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketPriceApplicationService marketPriceApplicationService;

    @PostMapping("/prices")
    public ApiResponse<PriceSnapshot> getPrice(@RequestBody PriceRequest request) {

        PriceRequestService priceRequestService = request.toPriceRequestService();

        PriceSnapshot snapshot = marketPriceApplicationService.getOrCreateSamplePrice(priceRequestService);
        return ApiResponse.ok(snapshot);
    }
}
