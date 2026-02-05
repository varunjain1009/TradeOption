package com.tradeoption.service.impl;

import com.tradeoption.domain.OptionLeg;
import com.tradeoption.domain.Strategy;
import com.tradeoption.service.PnlCalculatorService;
import com.tradeoption.service.ProbabilityService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
public class ProbabilityServiceImpl implements ProbabilityService {

    private final PnlCalculatorService pnlCalculatorService;

    public ProbabilityServiceImpl(PnlCalculatorService pnlCalculatorService) {
        this.pnlCalculatorService = pnlCalculatorService;
    }

    @Override
    public double calculateProbabilityOfProfit(Strategy strategy, double spot, double volatility, double timeToExpiry,
            double interestRate) {
        if (strategy.getLegs().isEmpty()) {
            return 0.0;
        }

        // 1. Find Breakeven Interval(s) where PNL > 0
        List<double[]> profitableIntervals = findProfitableIntervals(strategy);

        // 2. Sum Probability of ending up in those intervals
        double totalProbability = 0.0;
        for (double[] interval : profitableIntervals) {
            double lower = interval[0];
            double upper = interval[1];

            // Prob(Lower < St < Upper) = Prob(St < Upper) - Prob(St < Lower)
            // St is lognormally distributed
            // P(St < X) = N(d2(X))
            // where d2(X) = (ln(X/S) - (r - 0.5*sigma^2)*T) / (sigma*sqrt(T))
            // CAREFUL: Usually standard BS d2 is for Prob(ITM) which is N(d2).
            // But strictly, P(St > K) under risk-neutral measure is N(d2).
            // P(St < K) = N(-d2).

            // Let's use standard formula:
            // P(St < K) = N( (ln(K/S) - (r - 0.5*v^2)T) / (v*sqrt(T)) )
            // This is actually N(-d2_black_scholes) if we align terms carefully.
            // Let's implement calculateProbLessThan(target, spot, ...);

            double probUpper = (upper == Double.MAX_VALUE) ? 1.0
                    : calculateProbLessThan(upper, spot, volatility, timeToExpiry, interestRate);
            double probLower = (lower == Double.MIN_VALUE) ? 0.0
                    : calculateProbLessThan(lower, spot, volatility, timeToExpiry, interestRate);

            totalProbability += Math.max(0, probUpper - probLower);
        }

        return Math.min(1.0, Math.max(0.0, totalProbability));
    }

    private double calculateProbLessThan(double limit, double spot, double vol, double t, double r) {
        // P(St < K) = N( (ln(K/S) - (r - 0.5*v^2)T) / (v*sqrt(T)) )
        // Using ln(limit/spot)
        double numerator = Math.log(limit / spot) - (r - 0.5 * vol * vol) * t;
        double denominator = vol * Math.sqrt(t);
        double z = numerator / denominator;
        return cdf(z);
    }

    // Helper to find intervals [low, high] where Strategy PNL > 0
    private List<double[]> findProfitableIntervals(Strategy strategy) {
        List<double[]> intervals = new ArrayList<>();

        Set<Double> criticalPoints = new TreeSet<>();
        for (OptionLeg leg : strategy.getLegs()) {
            criticalPoints.add(leg.getStrikePrice());
        }

        List<Double> points = new ArrayList<>(criticalPoints);
        if (points.isEmpty())
            return intervals; // No legs

        // Add extreme points to check tails
        double minStrike = points.get(0);
        double maxStrike = points.get(points.size() - 1);

        // We will scan from well below min strike to well above max strike
        // Since payoff is linear between strikes, we check:
        // 1. Far Left Tail (below min strike)
        // 2. Between each adjacent strike pair
        // 3. Far Right Tail (above max strike)

        List<Double> checkPoints = new ArrayList<>();
        // Add a point far left: minStrike - 1000 (arbitrary large gap)
        // Ideally we check slope.
        // Let's simplify: Check PNL at each strike. Check PNL at min-epsilon and
        // max+epsilon.

        // Construct a list of points of interest:
        // [min - big, ...strikes..., max + big]
        // But better is to sweep and find zero crossings.

        // Let's iterate segments: (-inf, S1), (S1, S2), ..., (Sn, +inf)
        // Find zero crossing in each segment.

        double currentSpot = minStrike - 10000; // Start far left
        double currentPnl = pnlCalculatorService.calculateStrategyPnl(strategy, currentSpot);

        boolean isPositive = currentPnl > 0;
        double intervalStart = isPositive ? Double.MIN_VALUE : Double.NaN;

        // Combine checking logic. We need sorted checkpoints.
        List<Double> checkpoints = new ArrayList<>();
        checkpoints.add(minStrike - 10000); // Represents -Inf region
        checkpoints.addAll(points);
        checkpoints.add(maxStrike + 10000); // Represents +Inf region

        for (int i = 0; i < checkpoints.size() - 1; i++) {
            double p1 = checkpoints.get(i);
            double p2 = checkpoints.get(i + 1);

            double pnl1 = pnlCalculatorService.calculateStrategyPnl(strategy, p1);
            double pnl2 = pnlCalculatorService.calculateStrategyPnl(strategy, p2);

            // Analyze segment [p1, p2]

            // Case 1: Transition from Loss to Profit
            if (pnl1 <= 0 && pnl2 > 0) {
                // Find zero crossing
                double zeroX = interpolateZero(p1, pnl1, p2, pnl2);
                intervalStart = zeroX;
            }
            // Case 2: Transition from Profit to Loss
            else if (pnl1 > 0 && pnl2 <= 0) {
                // Find zero crossing
                double zeroX = interpolateZero(p1, pnl1, p2, pnl2);
                if (!Double.isNaN(intervalStart)) {
                    intervals.add(new double[] { intervalStart, zeroX });
                    intervalStart = Double.NaN; // Closed interval
                }
            }
            // Case 3: Stays Positive
            else if (pnl1 > 0 && pnl2 > 0) {
                // Was positive, stays positive.
                if (Double.isNaN(intervalStart)) {
                    // Should have started earlier, but maybe we just started effectively at -Inf
                    // If i==0, this means -Inf is profitable
                    if (i == 0)
                        intervalStart = Double.MIN_VALUE;
                }
            }
            // Case 4: Stays Negative, do nothing.
        }

        // Final check: if still open interval (Profit -> ... -> +Inf)
        if (!Double.isNaN(intervalStart)) {
            intervals.add(new double[] { intervalStart, Double.MAX_VALUE });
        }

        return intervals;
    }

    // Linear interpolation for zero crossing
    private double interpolateZero(double x1, double y1, double x2, double y2) {
        // y - y1 = m(x - x1)
        // m = (y2 - y1) / (x2 - x1)
        // Find x where y = 0
        // -y1 = m(x - x1) => x = x1 - y1/m
        if (Math.abs(y2 - y1) < 1e-9)
            return x1; // Flat line
        double m = (y2 - y1) / (x2 - x1);
        return x1 - (y1 / m);
    }

    // Standard Normal CDF (reused from Greeks/BS impl - ideally move to MathUtil)
    private double cdf(double x) {
        if (x < 0)
            return 1 - cdf(-x);
        double k = 1.0 / (1.0 + 0.2316419 * x);
        double poly = k * (0.319381530 + k * (-0.356563782 + k * (1.781477937 + k * (-1.821255978 + k * 1.330274429))));
        return 1.0 - pdf(x) * poly;
    }

    private double pdf(double x) {
        return Math.exp(-0.5 * x * x) / Math.sqrt(2 * Math.PI);
    }
}
