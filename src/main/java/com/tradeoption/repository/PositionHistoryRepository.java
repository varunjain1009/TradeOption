package com.tradeoption.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeoption.domain.PositionMetric;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class PositionHistoryRepository {

    private final RocksDBRepository rocksDBRepository;
    private final ObjectMapper objectMapper;
    private static final String KEY_PREFIX = "history:";

    public PositionHistoryRepository(RocksDBRepository rocksDBRepository, ObjectMapper objectMapper) {
        this.rocksDBRepository = rocksDBRepository;
        this.objectMapper = objectMapper;
    }

    public void addMetric(String positionId, PositionMetric metric) {
        String key = KEY_PREFIX + positionId;
        List<PositionMetric> history = getHistory(positionId);
        if (history == null) {
            history = new ArrayList<>();
        }
        history.add(metric);
        rocksDBRepository.save(key, history);
    }

    public List<PositionMetric> getHistory(String positionId) {
        // RocksDBRepository.find deserializes to a specific class.
        // For List<T>, we might need a wrapper or handle unchecked cast if
        // RocksDBRepository supports it via TypeReference or similar.
        // Looking at RocksDBRepository.find(String key, Class<T> clazz), it uses
        // objectMapper.readValue(bytes, clazz).
        // This won't work well for generics like List<PositionMetric>.
        // We'll need to fetch as basic object or array and map, OR extend
        // RocksDBRepository.

        // workaround: Since RocksDBRepository.find takes Class<T>, we can't easily pass
        // List.class with generic info.
        // However, we can use an array PositionMetric[].class -> List conversion.

        PositionMetric[] array = rocksDBRepository.find(KEY_PREFIX + positionId, PositionMetric[].class);
        if (array == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(List.of(array));
    }

    public void deleteHistory(String positionId) {
        rocksDBRepository.delete(KEY_PREFIX + positionId);
    }
}
