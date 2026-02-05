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
        // Mock implementation: returns a random price between 100 and 200
        return 100 + (200 - 100) * random.nextDouble();
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
