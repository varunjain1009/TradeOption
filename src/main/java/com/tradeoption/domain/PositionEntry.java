package com.tradeoption.domain;

import java.time.Instant;
import java.util.UUID;

public class PositionEntry {
    private String id;
    private double price;
    private int quantity;
    private TradeAction action;
    private long timestamp;

    private String linkedEntryId; // Optional: ID this entry is closing

    public PositionEntry() {
        // Default constructor for serialization
    }

    public PositionEntry(double price, int quantity, TradeAction action) {
        this.id = UUID.randomUUID().toString();
        this.price = price;
        this.quantity = quantity;
        this.action = action;
        this.timestamp = Instant.now().toEpochMilli();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public TradeAction getAction() {
        return action;
    }

    public void setAction(TradeAction action) {
        this.action = action;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getLinkedEntryId() {
        return linkedEntryId;
    }

    public void setLinkedEntryId(String linkedEntryId) {
        this.linkedEntryId = linkedEntryId;
    }
}
