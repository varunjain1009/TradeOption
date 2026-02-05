package com.tradeoption.domain;

public class ExitRequest {
    private double price;
    private int quantity;
    private String linkedEntryId;

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

    public String getLinkedEntryId() {
        return linkedEntryId;
    }

    public void setLinkedEntryId(String linkedEntryId) {
        this.linkedEntryId = linkedEntryId;
    }
}
