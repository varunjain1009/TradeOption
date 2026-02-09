package com.tradeoption.domain;

import java.util.ArrayList;
import java.util.List;

public class Strategy {
    private String id;
    private List<OptionLeg> legs = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private String symbol;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    private String name;
    private Long createdTimestamp;
    private Long updatedTimestamp;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public Long getUpdatedTimestamp() {
        return updatedTimestamp;
    }

    public void setUpdatedTimestamp(Long updatedTimestamp) {
        this.updatedTimestamp = updatedTimestamp;
    }

    private String status = "ACTIVE"; // ACTIVE, CLOSED

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private Long closedTimestamp;
    private Double realizedPnl;

    public Long getClosedTimestamp() {
        return closedTimestamp;
    }

    public void setClosedTimestamp(Long closedTimestamp) {
        this.closedTimestamp = closedTimestamp;
    }

    public Double getRealizedPnl() {
        return realizedPnl;
    }

    public void setRealizedPnl(Double realizedPnl) {
        this.realizedPnl = realizedPnl;
    }

    public void addLeg(OptionLeg leg) {
        this.legs.add(leg);
    }

    public List<OptionLeg> getLegs() {
        return legs;
    }
}
