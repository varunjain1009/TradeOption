package com.tradeoption.service;

import com.tradeoption.domain.Strategy;
import java.util.List;
import java.util.Optional;

public interface StrategyService {
    Strategy saveStrategy(Strategy strategy, String username);

    Optional<Strategy> getStrategy(String id, String username);

    List<Strategy> getAllStrategies(String username);

    void deleteStrategy(String id, String username);
}
