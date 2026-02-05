package com.tradeoption.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rocksdb.RocksDB;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class RocksDBRepositoryTest {

    private RocksDBRepository repository;
    private String testDbFile;

    @BeforeEach
    public void setUp() {
        testDbFile = "trade-option-db-test-" + java.util.UUID.randomUUID();
        // Ensure clean slate
        FileSystemUtils.deleteRecursively(new File(testDbFile));
        repository = new RocksDBRepository(new ObjectMapper(), testDbFile);
        repository.init();
    }

    @AfterEach
    public void tearDown() {
        repository.close();
        FileSystemUtils.deleteRecursively(new File(testDbFile));
    }

    @Test
    public void testSaveAndFind() {
        String key = "strategy-1";
        TestObject original = new TestObject("MyStrategy", 100);

        repository.save(key, original);

        TestObject retrieved = repository.find(key, TestObject.class);

        assertNotNull(retrieved);
        assertEquals(original.getName(), retrieved.getName());
        assertEquals(original.getValue(), retrieved.getValue());
    }

    @Test
    public void testDelete() {
        String key = "strategy-2";
        TestObject original = new TestObject("ToDelete", 50);

        repository.save(key, original);
        assertNotNull(repository.find(key, TestObject.class));

        repository.delete(key);
        assertNull(repository.find(key, TestObject.class));
    }

    // Simple POJO for testing
    static class TestObject {
        private String name;
        private int value;

        public TestObject() {
        } // Jackson needs empty constructor

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }
}
