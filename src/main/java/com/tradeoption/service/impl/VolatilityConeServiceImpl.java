package com.tradeoption.service.impl;

import com.tradeoption.domain.ConePoint;
import com.tradeoption.service.VolatilityConeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class VolatilityConeServiceImpl implements VolatilityConeService {

    @Override
    public List<ConePoint> generateCone(double spotPrice, double volatility, double timeHorizonYears, int steps) {
        List<ConePoint> conePoints = new ArrayList<>();

        // Ensure at least 1 step
        if (steps < 1)
            steps = 1;

        double dt = timeHorizonYears / steps;

        // Start from t=0 (current spot)
        // t=0, bounds are spot
        conePoints.add(new ConePoint(0, spotPrice, spotPrice, spotPrice, spotPrice));

        for (int i = 1; i <= steps; i++) {
            double t = i * dt;
            double stdDevMove = volatility * Math.sqrt(t);

            // Using Log-normal projection: S_t = S_0 * exp( +/- sigma*sqrt(t) )
            // Ignoring drift for pure volatility cone

            double upper1 = spotPrice * Math.exp(stdDevMove);
            double lower1 = spotPrice * Math.exp(-stdDevMove);

            double upper2 = spotPrice * Math.exp(2 * stdDevMove);
            double lower2 = spotPrice * Math.exp(-2 * stdDevMove);

            conePoints.add(new ConePoint(t, upper1, lower1, upper2, lower2));
        }

        return conePoints;
    }
}
