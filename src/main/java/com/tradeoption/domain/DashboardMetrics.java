package com.tradeoption.domain;

public class DashboardMetrics {
    private Greeks greeks;
    private double currentPnl;
    private double maxProfit;
    private double maxLoss;
    private double probabilityOfProfit;
    private double riskRewardRatio;
    private java.util.List<Double> legLtps;
    private long timestamp;

    public DashboardMetrics() {
    }

    public DashboardMetrics(Greeks greeks, double currentPnl, double maxProfit, double maxLoss,
            double probabilityOfProfit, double riskRewardRatio, long timestamp) {
        this(greeks, currentPnl, maxProfit, maxLoss, probabilityOfProfit, riskRewardRatio, null, timestamp);
    }

    public DashboardMetrics(Greeks greeks, double currentPnl, double maxProfit, double maxLoss,
            double probabilityOfProfit, double riskRewardRatio, java.util.List<Double> legLtps, long timestamp) {
        this.greeks = greeks;
        this.currentPnl = currentPnl;
        this.maxProfit = maxProfit;
        this.maxLoss = maxLoss;
        this.probabilityOfProfit = probabilityOfProfit;
        this.riskRewardRatio = riskRewardRatio;
        this.legLtps = legLtps;
        this.timestamp = timestamp;
    }

    public java.util.List<Double> getLegLtps() {
        return legLtps;
    }

    public void setLegLtps(java.util.List<Double> legLtps) {
        this.legLtps = legLtps;
    }

    public Greeks getGreeks() {
        return greeks;
    }

    public void setGreeks(Greeks greeks) {
        this.greeks = greeks;
    }

    public double getCurrentPnl() {
        return currentPnl;
    }

    public void setCurrentPnl(double currentPnl) {
        this.currentPnl = currentPnl;
    }

    public double getMaxProfit() {
        return maxProfit;
    }

    public void setMaxProfit(double maxProfit) {
        this.maxProfit = maxProfit;
    }

    public double getMaxLoss() {
        return maxLoss;
    }

    public void setMaxLoss(double maxLoss) {
        this.maxLoss = maxLoss;
    }

    public double getProbabilityOfProfit() {
        return probabilityOfProfit;
    }

    public void setProbabilityOfProfit(double probabilityOfProfit) {
        this.probabilityOfProfit = probabilityOfProfit;
    }

    public double getRiskRewardRatio() {
        return riskRewardRatio;
    }

    public void setRiskRewardRatio(double riskRewardRatio) {
        this.riskRewardRatio = riskRewardRatio;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    private boolean isClosed;

    public boolean isClosed() {
        return isClosed;
    }

    public void setClosed(boolean isClosed) {
        this.isClosed = isClosed;
    }
}
