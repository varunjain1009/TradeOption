package com.tradeoption.service.impl;

import com.tradeoption.domain.Strategy;
import com.tradeoption.repository.RocksDBRepository;
import com.tradeoption.service.StrategyService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StrategyServiceImpl implements StrategyService {

    private static final String KEY_PREFIX = "strategy:";
    private final RocksDBRepository rocksDBRepository;

    public StrategyServiceImpl(RocksDBRepository rocksDBRepository) {
        this.rocksDBRepository = rocksDBRepository;
    }

    @Override
    public Strategy saveStrategy(Strategy strategy, String username) {
        long now = System.currentTimeMillis();

        if (strategy.getId() == null || strategy.getId().isEmpty()) {
            strategy.setId(UUID.randomUUID().toString());
            strategy.setCreatedTimestamp(now);
        }

        // Preserve original creation time if updating
        if (strategy.getCreatedTimestamp() == null) {
            strategy.setCreatedTimestamp(now);
        }

        strategy.setUpdatedTimestamp(now);

        // Key structure: strategy:{username}:{id}
        String key = KEY_PREFIX + username + ":" + strategy.getId();
        rocksDBRepository.save(key, strategy);

        return strategy;
    }

    @Override
    public Optional<Strategy> getStrategy(String id, String username) {
        String key = KEY_PREFIX + username + ":" + id;
        Strategy strategy = rocksDBRepository.find(key, Strategy.class);
        return Optional.ofNullable(strategy);
    }

    @Override
    public List<Strategy> getAllStrategies(String username) {
        String userPrefix = KEY_PREFIX + username + ":";
        return rocksDBRepository.findByPrefix(userPrefix, Strategy.class)
                .stream()
                .sorted(Comparator.comparing(Strategy::getUpdatedTimestamp,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteStrategy(String id, String username) {
        String key = KEY_PREFIX + username + ":" + id;
        rocksDBRepository.delete(key);
    }
}
