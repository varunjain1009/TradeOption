package com.tradeoption.domain;

public class PositionPnl {
    private double realizedPnl;
    private double unrealizedPnl;
    private int netQuantity;
    private double averageOpenPrice;
    private double totalUnrealizedPnl; // Can distinguish if needed

    public PositionPnl(double realizedPnl, double unrealizedPnl, int netQuantity, double averageOpenPrice) {
        this.realizedPnl = realizedPnl;
        this.unrealizedPnl = unrealizedPnl;
        this.netQuantity = netQuantity;
        this.averageOpenPrice = averageOpenPrice;
    }

    // Getters
    public double getRealizedPnl() {
        return realizedPnl;
    }

    public double getUnrealizedPnl() {
        return unrealizedPnl;
    }

    public int getNetQuantity() {
        return netQuantity;
    }

    public double getAverageOpenPrice() {
        return averageOpenPrice;
    }
}
