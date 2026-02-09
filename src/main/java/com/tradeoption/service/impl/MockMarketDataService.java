package com.tradeoption.service.impl;

import com.tradeoption.domain.OptionLeg;
import com.tradeoption.service.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Service
public class MockMarketDataService implements MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MockMarketDataService.class);

    private final Random random = new Random();

    @Override
    public Optional<Double> getLtp(String symbol) {
        if ("FAIL".equalsIgnoreCase(symbol)) {
            return Optional.empty();
        }

        // Mock implementation with realistic ranges
        double base = 100;
        if (symbol != null) {
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
        }
        // Fluctuate by +/- 0.5%
        return Optional.of(base + (base * 0.005 * (random.nextDouble() * 2 - 1)));
    }

    @Override
    public Optional<Double> getLtp(OptionLeg leg) {
        if ("FAIL".equalsIgnoreCase(leg.getSymbol())) {
            return Optional.empty();
        }

        Optional<Double> spotOpt = getLtp(leg.getSymbol() != null ? leg.getSymbol() : "NIFTY");
        if (spotOpt.isEmpty())
            return Optional.empty();

        double spot = spotOpt.get();
        double strike = leg.getStrikePrice();
        double intrinsic = 0.0;

        if (leg.getType() == com.tradeoption.domain.LegType.CE) {
            intrinsic = Math.max(0, spot - strike);
        } else {
            intrinsic = Math.max(0, strike - spot);
        }

        // Time Value Approximation (1% of Spot, decaying)
        // Ideally should use Black-Scholes but simple mock is enough for now
        double timeValue = spot * 0.01;

        // Add some noise
        double basePrice = intrinsic + timeValue;
        double noise = basePrice * 0.02 * (random.nextDouble() * 2 - 1);
        double price = Math.max(0.1, basePrice + noise);

        log.trace("Calculated Price for {} {}: Intrinsic={}, TimeVal={}, Noise={}, Final={}",
                leg.getSymbol(), leg.getStrikePrice(), intrinsic, timeValue, noise, price);

        return Optional.of(price);
    }

    @Override
    public Optional<Double> getBid(OptionLeg leg) {
        return getLtp(leg).map(p -> p * 0.999); // Simple mock bid
    }

    @Override
    public Optional<Double> getAsk(OptionLeg leg) {
        return getLtp(leg).map(p -> p * 1.001); // Simple mock ask
    }
}
