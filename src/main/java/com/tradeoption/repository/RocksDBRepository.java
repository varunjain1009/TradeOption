package com.tradeoption.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Repository
public class RocksDBRepository {

    private static final Logger logger = LoggerFactory.getLogger(RocksDBRepository.class);
    private final String dbFile;
    private RocksDB db;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public RocksDBRepository(ObjectMapper objectMapper) {
        this(objectMapper, "trade-option-db");
    }

    public RocksDBRepository(ObjectMapper objectMapper, String dbFile) {
        this.objectMapper = objectMapper;
        this.dbFile = dbFile;
    }

    @PostConstruct
    public void init() {
        Options options = new Options();
        options.setCreateIfMissing(true);
        try {
            // Ensure directory exists if path is deeper, but here likely just local folder
            db = RocksDB.open(options, dbFile);
            logger.info("RocksDB initialized at: {}", dbFile);
        } catch (RocksDBException e) {
            logger.error("Error initializing RocksDB", e);
            throw new RuntimeException("Failed to initialize RocksDB", e);
        } finally {
            options.close();
        }
    }

    @PreDestroy
    public void close() {
        if (db != null) {
            db.close();
            logger.info("RocksDB closed.");
        }
    }

    public void save(String key, Object value) {
        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            byte[] valueBytes = objectMapper.writeValueAsBytes(value);
            db.put(keyBytes, valueBytes);
        } catch (RocksDBException | JsonProcessingException e) {
            throw new RuntimeException("Error saving to RocksDB", e);
        }
    }

    public <T> T find(String key, Class<T> clazz) {
        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            byte[] valueBytes = db.get(keyBytes);
            if (valueBytes == null) {
                return null;
            }
            return objectMapper.readValue(valueBytes, clazz);
        } catch (RocksDBException | IOException e) {
            throw new RuntimeException("Error reading from RocksDB", e);
        }
    }

    public void delete(String key) {
        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            db.delete(keyBytes);
        } catch (RocksDBException e) {
            throw new RuntimeException("Error deleting from RocksDB", e);
        }
    }

    public <T> java.util.List<T> findByPrefix(String prefix, Class<T> clazz) {
        java.util.List<T> results = new java.util.ArrayList<>();
        try (org.rocksdb.RocksIterator iterator = db.newIterator()) {
            byte[] prefixBytes = prefix.getBytes(StandardCharsets.UTF_8);
            for (iterator.seek(prefixBytes); iterator.isValid(); iterator.next()) {
                byte[] key = iterator.key();
                // Check if key still matches prefix
                if (!startsWith(key, prefixBytes)) {
                    break;
                }
                try {
                    T value = objectMapper.readValue(iterator.value(), clazz);
                    results.add(value);
                } catch (IOException e) {
                    logger.error("Failed to deserialize value for key: " + new String(key), e);
                }
            }
        }
        return results;
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length)
            return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i])
                return false;
        }
        return true;
    }
}
