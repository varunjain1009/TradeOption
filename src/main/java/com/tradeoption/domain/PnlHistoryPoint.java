package com.tradeoption.domain;

public class PnlHistoryPoint {
    private long timestamp;
    private double realizedPnl;

    public PnlHistoryPoint(long timestamp, double realizedPnl) {
        this.timestamp = timestamp;
        this.realizedPnl = realizedPnl;
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
}
