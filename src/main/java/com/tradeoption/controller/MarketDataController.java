package com.tradeoption.controller;

import com.tradeoption.domain.LegType;
import com.tradeoption.domain.OptionLeg;
import com.tradeoption.domain.TradeAction;
import com.tradeoption.service.MarketDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market-data")
public class MarketDataController {

    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/quote")
    public ResponseEntity<Double> getQuote(
            @RequestParam String symbol,
            @RequestParam String expiry,
            @RequestParam double strike,
            @RequestParam String type,
            @RequestParam String action) {

        try {
            LegType legType = LegType.valueOf(type.toUpperCase());
            TradeAction tradeAction = TradeAction.valueOf(action.toUpperCase());

            // Temporary leg object for query
            OptionLeg leg = new OptionLeg(strike, legType, tradeAction, 0.0, 1, expiry, symbol);

            java.util.Optional<Double> priceOpt;
            if (tradeAction == TradeAction.BUY) {
                priceOpt = marketDataService.getAsk(leg);
            } else {
                priceOpt = marketDataService.getBid(leg);
            }

            // Final Fallback to LTP if empty
            if (priceOpt.isEmpty() || priceOpt.get() == 0) {
                priceOpt = marketDataService.getLtp(leg);
            }

            return ResponseEntity.ok(priceOpt.orElse(0.0));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(0.0);
        }
    }
}
