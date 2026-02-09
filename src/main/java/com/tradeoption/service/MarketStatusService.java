package com.tradeoption.service;

public interface MarketStatusService {
    boolean isMarketOpen();

    String getMarketStatusReason();
}
