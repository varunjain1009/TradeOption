package com.tradeoption.repository;

import com.tradeoption.domain.Position;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PositionRepository {

    private final RocksDBRepository rocksDBRepository;
    private static final String PREFIX = "POS_";

    public PositionRepository(RocksDBRepository rocksDBRepository) {
        this.rocksDBRepository = rocksDBRepository;
    }

    public void save(Position position) {
        // Key format: POS_Symbol_Expiry_Strike_Type
        String key = PREFIX + position.getId();
        rocksDBRepository.save(key, position);
    }

    public Position findById(String id) {
        return rocksDBRepository.find(PREFIX + id, Position.class);
    }

    public List<Position> findAll() {
        return rocksDBRepository.findByPrefix(PREFIX, Position.class);
    }

    public void delete(String id) {
        rocksDBRepository.delete(PREFIX + id);
    }
}
