package com.tradeoption.domain;

import java.util.HashMap;
import java.util.Map;

public class SystemConfig {
    private double riskFreeRate = 0.05; // Default 5%
    private Map<String, Integer> lotSizes = new HashMap<>();
    private Map<String, Double> bidAskConstraints = new HashMap<>();
    private long refreshIntervalMs = 1000;

    // Dynamic Dropdowns: Map<Symbol, List<ExpiryDates>>
    private java.util.Map<String, java.util.List<String>> symbolExpiries = new java.util.HashMap<>();

    public double getRiskFreeRate() {
        return riskFreeRate;
    }

    public void setRiskFreeRate(double riskFreeRate) {
        this.riskFreeRate = riskFreeRate;
    }

    public Map<String, Integer> getLotSizes() {
        return lotSizes;
    }

    public void setLotSizes(Map<String, Integer> lotSizes) {
        this.lotSizes = lotSizes;
    }

    public Map<String, Double> getBidAskConstraints() {
        return bidAskConstraints;
    }

    public void setBidAskConstraints(Map<String, Double> bidAskConstraints) {
        this.bidAskConstraints = bidAskConstraints;
    }

    public long getRefreshIntervalMs() {
        return refreshIntervalMs;
    }

    public void setRefreshIntervalMs(long refreshIntervalMs) {
        this.refreshIntervalMs = refreshIntervalMs;
    }

    public java.util.Map<String, java.util.List<String>> getSymbolExpiries() {
        return symbolExpiries;
    }

    public void setSymbolExpiries(java.util.Map<String, java.util.List<String>> symbolExpiries) {
        this.symbolExpiries = symbolExpiries;
    }
}
