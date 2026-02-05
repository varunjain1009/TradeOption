package com.tradeoption.domain;

public class OptionLeg {
    private double strikePrice;
    private LegType type;
    private TradeAction action;
    private double entryPrice;
    private int quantity;

    public OptionLeg(double strikePrice, LegType type, TradeAction action, double entryPrice, int quantity) {
        this.strikePrice = strikePrice;
        this.type = type;
        this.action = action;
        this.entryPrice = entryPrice;
        this.quantity = quantity;
    }

    public double getStrikePrice() {
        return strikePrice;
    }

    public LegType getType() {
        return type;
    }

    public TradeAction getAction() {
        return action;
    }

    public double getEntryPrice() {
        return entryPrice;
    }

    public int getQuantity() {
        return quantity;
    }
}
