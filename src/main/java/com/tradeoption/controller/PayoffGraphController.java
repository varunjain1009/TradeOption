package com.tradeoption.controller;

import com.tradeoption.domain.PayoffGraphData;
import com.tradeoption.domain.Strategy;
import com.tradeoption.service.PayoffGraphService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
public class PayoffGraphController {

    private final PayoffGraphService payoffGraphService;

    public PayoffGraphController(PayoffGraphService payoffGraphService) {
        this.payoffGraphService = payoffGraphService;
    }

    @PostMapping("/payoff-graph")
    public PayoffGraphData getPayoffGraph(
            @RequestBody Strategy strategy,
            @RequestParam double spot,
            @RequestParam double volatility,
            @RequestParam double timeToExpiry,
            @RequestParam double interestRate,
            @RequestParam(defaultValue = "0.2") double range) {

        return payoffGraphService.generatePayoffGraph(strategy, spot, volatility, timeToExpiry, interestRate, range);
    }
}
