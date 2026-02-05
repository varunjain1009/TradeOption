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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Repository
public class RocksDBRepository {

    private static final Logger logger = LoggerFactory.getLogger(RocksDBRepository.class);
    private static final String DB_FILE = "trade-option-db";

    // Static load of the library
    static {
        RocksDB.loadLibrary();
    }

    private RocksDB db;
    private final ObjectMapper objectMapper;

    public RocksDBRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        Options options = new Options().setCreateIfMissing(true);
        try {
            // Ensure directory exists if path is deeper, but here likely just local folder
            db = RocksDB.open(options, DB_FILE);
            logger.info("RocksDB initialized at: {}", DB_FILE);
        } catch (RocksDBException e) {
            logger.error("Error initializing RocksDB", e);
            throw new RuntimeException("Failed to initialize RocksDB", e);
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
}
