package com.tradeoption.domain;

public class OptionLeg {
    private double strikePrice;
    private LegType type;
    private TradeAction action;
    private double entryPrice;
    private Double exitPrice; // Nullable - only set when position is closed
    private int quantity;
    private String expiryDate;
    private String symbol;

    public OptionLeg() {
    }

    public OptionLeg(double strikePrice, LegType type, TradeAction action, double entryPrice, int quantity,
            String expiryDate, String symbol) {
        this.strikePrice = strikePrice;
        this.type = type;
        this.action = action;
        this.entryPrice = entryPrice;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
        this.symbol = symbol;
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

    public double getPrice() {
        return entryPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setStrikePrice(double strikePrice) {
        this.strikePrice = strikePrice;
    }

    public void setType(LegType type) {
        this.type = type;
    }

    public void setAction(TradeAction action) {
        this.action = action;
    }

    public void setEntryPrice(double entryPrice) {
        this.entryPrice = entryPrice;
    }

    public Double getExitPrice() {
        return exitPrice;
    }

    public void setExitPrice(Double exitPrice) {
        this.exitPrice = exitPrice;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
