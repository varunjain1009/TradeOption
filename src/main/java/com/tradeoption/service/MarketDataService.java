package com.tradeoption.service;

import com.tradeoption.domain.OptionLeg;

public interface MarketDataService {
    double getLtp(String symbol);

    // Helper to generate a symbol key from an option leg if necessary
    // defaulting to simple string for now, but good to have abstraction
    double getLtp(OptionLeg leg);
}
