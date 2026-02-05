package com.tradeoption.domain;

import java.util.ArrayList;
import java.util.List;

public class Strategy {
    private List<OptionLeg> legs = new ArrayList<>();

    public void addLeg(OptionLeg leg) {
        this.legs.add(leg);
    }

    public List<OptionLeg> getLegs() {
        return legs;
    }
}
