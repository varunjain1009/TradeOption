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

    public void addLeg(OptionLeg leg) {
        this.legs.add(leg);
    }

    public List<OptionLeg> getLegs() {
        return legs;
    }
}
