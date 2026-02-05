package com.tradeoption.domain;

import java.time.LocalDateTime;

public class MarketData {
    private String symbol;
    private double ltp;
    private LocalDateTime timestamp;

    public MarketData(String symbol, double ltp, LocalDateTime timestamp) {
        this.symbol = symbol;
        this.ltp = ltp;
        this.timestamp = timestamp;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getLtp() {
        return ltp;
    }

    public void setLtp(double ltp) {
        this.ltp = ltp;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
