package com.tradeoption.service;

import com.tradeoption.domain.OptionLeg;

public interface MarketDataService {
    java.util.Optional<Double> getLtp(String symbol);

    // Helper to generate a symbol key from an option leg if necessary
    // defaulting to simple string for now, but good to have abstraction
    java.util.Optional<Double> getLtp(OptionLeg leg);

    java.util.Optional<Double> getBid(OptionLeg leg);

    java.util.Optional<Double> getAsk(OptionLeg leg);
}
