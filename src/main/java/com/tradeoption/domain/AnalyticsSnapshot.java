package com.tradeoption.domain;

public class AnalyticsSnapshot {
    private long timestamp;
    private String strategyId;
    private double pnl;
    private Greeks greeks;
    private double spotPrice;

    public AnalyticsSnapshot() {
    }

    public AnalyticsSnapshot(long timestamp, String strategyId, double pnl, Greeks greeks, double spotPrice) {
        this.timestamp = timestamp;
        this.strategyId = strategyId;
        this.pnl = pnl;
        this.greeks = greeks;
        this.spotPrice = spotPrice;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }

    public double getPnl() {
        return pnl;
    }

    public void setPnl(double pnl) {
        this.pnl = pnl;
    }

    public Greeks getGreeks() {
        return greeks;
    }

    public void setGreeks(Greeks greeks) {
        this.greeks = greeks;
    }

    public double getSpotPrice() {
        return spotPrice;
    }

    public void setSpotPrice(double spotPrice) {
        this.spotPrice = spotPrice;
    }
}
