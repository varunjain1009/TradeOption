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

    public void addLeg(OptionLeg leg) {
        this.legs.add(leg);
    }

    public List<OptionLeg> getLegs() {
        return legs;
    }
}
