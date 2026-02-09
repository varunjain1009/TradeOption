package com.tradeoption.domain;

public class PositionMetric {
    private long timestamp;
    private double realizedPnl;
    private double unrealizedPnl;
    private double underlyingPrice;
    private Greeks greeks;

    public PositionMetric() {
    }

    public PositionMetric(long timestamp, double realizedPnl, double unrealizedPnl, double underlyingPrice,
            Greeks greeks) {
        this.timestamp = timestamp;
        this.realizedPnl = realizedPnl;
        this.unrealizedPnl = unrealizedPnl;
        this.underlyingPrice = underlyingPrice;
        this.greeks = greeks;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getRealizedPnl() {
        return realizedPnl;
    }

    public void setRealizedPnl(double realizedPnl) {
        this.realizedPnl = realizedPnl;
    }

    public double getUnrealizedPnl() {
        return unrealizedPnl;
    }

    public void setUnrealizedPnl(double unrealizedPnl) {
        this.unrealizedPnl = unrealizedPnl;
    }

    public double getUnderlyingPrice() {
        return underlyingPrice;
    }

    public void setUnderlyingPrice(double underlyingPrice) {
        this.underlyingPrice = underlyingPrice;
    }

    public Greeks getGreeks() {
        return greeks;
    }

    public void setGreeks(Greeks greeks) {
        this.greeks = greeks;
    }
}
