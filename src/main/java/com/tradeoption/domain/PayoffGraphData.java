package com.tradeoption.domain;

import java.util.List;

public class PayoffGraphData {
    private List<Double> spotPrices;
    private List<Double> expiryPnl;
    private List<Double> tZeroPnl;
    private List<Double> breakevens;
    private Double netCredit;

    public PayoffGraphData() {
    }

    public PayoffGraphData(List<Double> spotPrices, List<Double> expiryPnl, List<Double> tZeroPnl,
            List<Double> breakevens, Double netCredit) {
        this.spotPrices = spotPrices;
        this.expiryPnl = expiryPnl;
        this.tZeroPnl = tZeroPnl;
        this.breakevens = breakevens;
        this.netCredit = netCredit;
    }

    public List<Double> getSpotPrices() {
        return spotPrices;
    }

    public void setSpotPrices(List<Double> spotPrices) {
        this.spotPrices = spotPrices;
    }

    public List<Double> getExpiryPnl() {
        return expiryPnl;
    }

    public void setExpiryPnl(List<Double> expiryPnl) {
        this.expiryPnl = expiryPnl;
    }

    public List<Double> getTZeroPnl() {
        return tZeroPnl;
    }

    public void setTZeroPnl(List<Double> tZeroPnl) {
        this.tZeroPnl = tZeroPnl;
    }

    public List<Double> getBreakevens() {
        return breakevens;
    }

    public void setBreakevens(List<Double> breakevens) {
        this.breakevens = breakevens;
    }

    public Double getNetCredit() {
        return netCredit;
    }

    public void setNetCredit(Double netCredit) {
        this.netCredit = netCredit;
    }
}
