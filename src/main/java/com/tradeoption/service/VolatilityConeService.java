package com.tradeoption.service;

import com.tradeoption.domain.ConePoint;
import java.util.List;

public interface VolatilityConeService {
    /**
     * Generates a volatility cone (price projection) over a time horizon.
     *
     * @param spotPrice        Current spot price
     * @param volatility       Annualized volatility (e.g., 0.20)
     * @param timeHorizonYears Total time to project (e.g., 1.0 for 1 year)
     * @param steps            Number of time steps to calculate
     * @return List of ConePoint
     */
    List<ConePoint> generateCone(double spotPrice, double volatility, double timeHorizonYears, int steps);
}
