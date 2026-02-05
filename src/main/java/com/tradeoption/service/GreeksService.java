package com.tradeoption.service;

import com.tradeoption.domain.Greeks;
import com.tradeoption.domain.OptionLeg;
import com.tradeoption.domain.Strategy;

public interface GreeksService {

    Greeks calculateGreeks(double spot, double strike, double timeToExpiry, double volatility, double interestRate,
            boolean isCall);

    Greeks calculateLegGreeks(OptionLeg leg, double spot, double volatility, double interestRate, double timeToExpiry);

    Greeks calculateStrategyGreeks(Strategy strategy, double spot, double volatility, double interestRate,
            double timeToExpiry);
}
