package com.tradeoption.service.impl;

import com.tradeoption.domain.OptionLeg;
import com.tradeoption.service.MarketDataService;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class MockMarketDataService implements MarketDataService {

    private final Random random = new Random();

    @Override
    public double getLtp(String symbol) {
        // Mock implementation with realistic ranges
        double base = 100;
        switch (symbol.toUpperCase()) {
            case "NIFTY":
                base = 22000;
                break;
            case "BANKNIFTY":
                base = 47000;
                break;
            case "GOLD":
                base = 62000;
                break;
            case "SILVER":
                base = 72000;
                break;
            case "CRUDEOIL":
                base = 6000;
                break;
            case "NATURALGAS":
                base = 150;
                break;
            case "COPPER":
                base = 720;
                break;
            default:
                base = 100;
        }
        // Fluctuate by +/- 0.5%
        return base + (base * 0.005 * (random.nextDouble() * 2 - 1));
    }

    @Override
    public double getLtp(OptionLeg leg) {
        // For a more realistic mock, we could use the strike price as a base
        // and fluctuate around it.
        double basePrice = leg.getStrikePrice();
        // Fluctuate by +/- 5%
        double variance = basePrice * 0.05;
        return basePrice + (random.nextDouble() * variance * 2 - variance);
    }
}
