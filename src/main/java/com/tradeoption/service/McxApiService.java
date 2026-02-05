package com.tradeoption.service;

public interface McxApiService {
    String getOptionChain(String commodity, String expiry);

    String getOptionChainStrikePrice(String commodity, String expiry, String strike);
}
