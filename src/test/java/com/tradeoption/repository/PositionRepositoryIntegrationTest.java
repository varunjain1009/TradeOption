package com.tradeoption.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeoption.domain.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rocksdb.RocksDB;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PositionRepositoryIntegrationTest {

    private RocksDBRepository rocksDBRepository;
    private PositionRepository positionRepository;
    private final String TEST_DB = "test-db-" + System.currentTimeMillis();

    @BeforeEach
    void setUp() {
        // Use a test-specific DB file
        rocksDBRepository = new RocksDBRepository(new ObjectMapper(), TEST_DB);
        rocksDBRepository.init();
        positionRepository = new PositionRepository(rocksDBRepository);
    }

    @AfterEach
    void tearDown() throws IOException {
        rocksDBRepository.close();
        // Clean up DB directory
        deleteDirectory(new File(TEST_DB));
    }

    private void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            Files.walk(directory.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    void testSaveAndFindPosition() {
        // Create a position
        Position position = new Position("NIFTY", "28MAR2024", 22000.0, LegType.CE);

        // Add entries
        PositionEntry entry1 = new PositionEntry(150.5, 50, TradeAction.BUY);
        PositionEntry entry2 = new PositionEntry(160.0, 50, TradeAction.SELL);

        position.addEntry(entry1);
        position.addEntry(entry2);

        // Save
        positionRepository.save(position);

        // Retrieve
        Position retrieved = positionRepository.findById(position.getId());

        assertNotNull(retrieved);
        assertEquals("NIFTY", retrieved.getSymbol());
        assertEquals(2, retrieved.getEntries().size());
        assertEquals(entry1.getPrice(), retrieved.getEntries().get(0).getPrice());
        assertEquals(0, retrieved.getNetQuantity()); // 50 Buy - 50 Sell = 0
    }

    @Test
    void testFindAll() {
        Position p1 = new Position("GOLD", "26APR2024", 60000, LegType.PE);
        Position p2 = new Position("SILVER", "26APR2024", 70000, LegType.CE);

        positionRepository.save(p1);
        positionRepository.save(p2);

        List<Position> all = positionRepository.findAll();
        assertEquals(2, all.size());
    }
}
