package com.tradeoption.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tradeoption.config.MathExpressionDeserializer;

import java.util.HashMap;
import java.util.Map;

public class SystemConfig {
    private double riskFreeRate = 0.05; // Default 5%
    private Map<String, Integer> lotSizes = new HashMap<>();
    private Map<String, Double> bidAskConstraints = new HashMap<>();
    private double maxBidAskDiffPercent = 5.0; // Default 5%

    @JsonDeserialize(using = MathExpressionDeserializer.class)
    private long refreshIntervalMs = 1000;

    @JsonDeserialize(using = MathExpressionDeserializer.class)
    private long spotBroadcastIntervalMs = 5000; // Default 5 seconds

    // Dynamic Dropdowns: Map<Symbol, List<ExpiryDates>>
    private java.util.Map<String, java.util.List<String>> symbolExpiries = new java.util.HashMap<>();

    private java.util.List<String> holidays = new java.util.ArrayList<>();
    private java.util.List<SpecialSession> specialSessions = new java.util.ArrayList<>();
    private boolean enableMockFallback = false; // Default to false as per user request

    // Market Hours Configuration
    private String marketOpenTime = "09:00";
    private String marketCloseTime = "23:45";
    private java.util.List<String> marketClosedDays = new java.util.ArrayList<>(
            java.util.Arrays.asList("SUNDAY", "MONDAY"));

    public String getMarketOpenTime() {
        return marketOpenTime;
    }

    public void setMarketOpenTime(String marketOpenTime) {
        this.marketOpenTime = marketOpenTime;
    }

    public String getMarketCloseTime() {
        return marketCloseTime;
    }

    public void setMarketCloseTime(String marketCloseTime) {
        this.marketCloseTime = marketCloseTime;
    }

    public java.util.List<String> getMarketClosedDays() {
        return marketClosedDays;
    }

    public void setMarketClosedDays(java.util.List<String> marketClosedDays) {
        this.marketClosedDays = marketClosedDays;
    }

    public boolean isEnableMockFallback() {
        return enableMockFallback;
    }

    public void setEnableMockFallback(boolean enableMockFallback) {
        this.enableMockFallback = enableMockFallback;
    }

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

    public double getMaxBidAskDiffPercent() {
        return maxBidAskDiffPercent;
    }

    public void setMaxBidAskDiffPercent(double maxBidAskDiffPercent) {
        this.maxBidAskDiffPercent = maxBidAskDiffPercent;
    }

    public long getRefreshIntervalMs() {
        return refreshIntervalMs;
    }

    public void setRefreshIntervalMs(long refreshIntervalMs) {
        this.refreshIntervalMs = refreshIntervalMs;
    }

    public long getSpotBroadcastIntervalMs() {
        return spotBroadcastIntervalMs;
    }

    public void setSpotBroadcastIntervalMs(long spotBroadcastIntervalMs) {
        this.spotBroadcastIntervalMs = spotBroadcastIntervalMs;
    }

    public java.util.Map<String, java.util.List<String>> getSymbolExpiries() {
        return symbolExpiries;
    }

    public void setSymbolExpiries(java.util.Map<String, java.util.List<String>> symbolExpiries) {
        this.symbolExpiries = symbolExpiries;
    }

    public java.util.List<String> getHolidays() {
        return holidays;
    }

    public void setHolidays(java.util.List<String> holidays) {
        this.holidays = holidays;
    }

    public java.util.List<SpecialSession> getSpecialSessions() {
        return specialSessions;
    }

    public void setSpecialSessions(java.util.List<SpecialSession> specialSessions) {
        this.specialSessions = specialSessions;
    }

    public static class SpecialSession {
        private String date; // YYYY-MM-DD
        private String startTime; // HH:mm
        private String endTime; // HH:mm
        private String reason;

        public SpecialSession() {
        }

        public SpecialSession(String date, String startTime, String endTime, String reason) {
            this.date = date;
            this.startTime = startTime;
            this.endTime = endTime;
            this.reason = reason;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
