package com.tradeoption.service.impl;

import com.tradeoption.domain.Greeks;
import com.tradeoption.domain.LegType;
import com.tradeoption.domain.OptionLeg;
import com.tradeoption.domain.Strategy;
import com.tradeoption.domain.TradeAction;
import com.tradeoption.service.GreeksService;
import org.springframework.stereotype.Service;

@Service
public class GreeksServiceImpl implements GreeksService {

    @Override
    public Greeks calculateGreeks(double S, double K, double T, double sigma, double r, boolean isCall) {
        if (T <= 0) {
            return new Greeks(0, 0, 0, 0, 0); // Expiry
        }

        double sqrtT = Math.sqrt(T);
        double d1 = (Math.log(S / K) + (r + 0.5 * sigma * sigma) * T) / (sigma * sqrtT);
        double d2 = d1 - sigma * sqrtT;

        double nd1 = pdf(d1);
        double Nd1 = cdf(d1);
        // double Nd2 = cdf(d2); // Not strictly needed for Greeks shown below but often
        // used

        double delta;
        double theta;
        double rho;

        if (isCall) {
            delta = Nd1;
            // Theta for call: - (S*n(d1)*sigma)/(2*sqrt(T)) - r*K*e^(-rT)*N(d2)
            theta = -(S * nd1 * sigma) / (2 * sqrtT) - r * K * Math.exp(-r * T) * cdf(d2);
            rho = K * T * Math.exp(-r * T) * cdf(d2);
        } else {
            delta = Nd1 - 1;
            // Theta for put: - (S*n(d1)*sigma)/(2*sqrt(T)) + r*K*e^(-rT)*N(-d2)
            theta = -(S * nd1 * sigma) / (2 * sqrtT) + r * K * Math.exp(-r * T) * cdf(-d2);
            rho = -K * T * Math.exp(-r * T) * cdf(-d2);
        }

        double gamma = nd1 / (S * sigma * sqrtT);
        double vega = S * nd1 * sqrtT;

        // Convert Theta to "per day" approximation if T is in years, standard
        // convention is often per year,
        // but traders prefer per day. Let's keep it strictly per year formula output
        // for now
        // or dividing by 365 is common practice for display.
        // Requirement says "Theta (daily decay)". Let's divide by 365.
        theta = theta / 365.0;

        // Vega is usually expressed as sensitivity to 1% change in vol.
        // Formula gives change for 100% change (sigma=1). So typically divide by 100.
        vega = vega / 100.0;

        return new Greeks(delta, gamma, theta, vega, rho);
    }

    @Override
    public Greeks calculateLegGreeks(OptionLeg leg, double spot, double volatility, double interestRate,
            double timeToExpiry) {
        boolean isCall = leg.getType() == LegType.CE;
        Greeks greeks = calculateGreeks(spot, leg.getStrikePrice(), timeToExpiry, volatility, interestRate, isCall);

        // Adjust for position (Long/Short) and Quantity
        int quantity = leg.getQuantity();
        double multiplier = (leg.getAction() == TradeAction.BUY) ? 1.0 : -1.0;

        greeks.setDelta(greeks.getDelta() * quantity * multiplier);
        greeks.setGamma(greeks.getGamma() * quantity * multiplier);
        greeks.setTheta(greeks.getTheta() * quantity * multiplier);
        greeks.setVega(greeks.getVega() * quantity * multiplier);
        greeks.setRho(greeks.getRho() * quantity * multiplier);

        return greeks;
    }

    @Override
    public Greeks calculateStrategyGreeks(Strategy strategy, double spot, double volatility, double interestRate,
            double timeToExpiry) {
        Greeks total = new Greeks();
        for (OptionLeg leg : strategy.getLegs()) {
            // Note: In real world, each leg might have different IV.
            // For now assuming constant volatility for the whole strategy or provided
            // externally.
            total.add(calculateLegGreeks(leg, spot, volatility, interestRate, timeToExpiry));
        }
        return total;
    }

    // Reuse math from BlackScholesServiceImpl (or extract to common util in
    // refactor)
    private double pdf(double x) {
        return Math.exp(-0.5 * x * x) / Math.sqrt(2 * Math.PI);
    }

    private double cdf(double x) {
        if (x < 0) {
            return 1 - cdf(-x);
        }
        double k = 1.0 / (1.0 + 0.2316419 * x);
        double poly = k * (0.319381530 + k * (-0.356563782 + k * (1.781477937 + k * (-1.821255978 + k * 1.330274429))));
        return 1.0 - pdf(x) * poly;
    }
}
