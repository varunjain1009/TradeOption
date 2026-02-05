package com.tradeoption.service;

import com.tradeoption.domain.Strategy;

public interface StrategySuggestionService {
    /**
     * Suggests a Straddle (Iron Fly) strategy for the given symbol.
     * 
     * @param symbol The symbol (e.g., "NIFTY")
     * @return A proposed Strategy object with legs populated.
     */
    Strategy suggestStraddle(String symbol);

    /**
     * Suggests a Strangle (Iron Condor) strategy for the given symbol.
     * 
     * @param symbol The symbol (e.g., "NIFTY")
     * @return A proposed Strategy object with legs populated.
     */
    Strategy suggestStrangle(String symbol);
}
