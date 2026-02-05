package com.tradeoption.domain;

import java.util.ArrayList;
import java.util.List;

public class Position {
    private String id; // Unique key: Symbol_Expiry_Strike_Type
    private String symbol;
    private String expiryDate;
    private double strikePrice;
    private LegType optionType;
    private List<PositionEntry> entries = new ArrayList<>();

    public Position() {
        // Default constructor
    }

    public Position(String symbol, String expiryDate, double strikePrice, LegType optionType) {
        this.symbol = symbol;
        this.expiryDate = expiryDate;
        this.strikePrice = strikePrice;
        this.optionType = optionType;
        this.id = generateId(symbol, expiryDate, strikePrice, optionType);
    }

    public static String generateId(String symbol, String expiryDate, double strikePrice, LegType optionType) {
        return String.format("%s_%s_%.2f_%s", symbol, expiryDate, strikePrice, optionType);
    }

    public void addEntry(PositionEntry entry) {
        this.entries.add(entry);
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public int getNetQuantity() {
        int netQty = 0;
        for (PositionEntry entry : entries) {
            if (entry.getAction() == TradeAction.BUY) {
                netQty += entry.getQuantity();
            } else {
                netQty -= entry.getQuantity();
            }
        }
        return netQty;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public double getAveragePrice() {
        double totalCost = 0;
        int totalQty = 0;
        // Simple avg price logic (can be refined for realized/unrealized later)
        // Ideally avg price is per side or weighted avg of open pos.
        // For Story 9.1 simple aggregation is enough.
        for (PositionEntry entry : entries) {
            totalCost += (entry.getPrice() * entry.getQuantity());
            totalQty += entry.getQuantity();
        }
        return totalQty == 0 ? 0 : totalCost / totalQty;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public double getStrikePrice() {
        return strikePrice;
    }

    public void setStrikePrice(double strikePrice) {
        this.strikePrice = strikePrice;
    }

    public LegType getOptionType() {
        return optionType;
    }

    public void setOptionType(LegType optionType) {
        this.optionType = optionType;
    }

    public List<PositionEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<PositionEntry> entries) {
        this.entries = entries;
    }
}
