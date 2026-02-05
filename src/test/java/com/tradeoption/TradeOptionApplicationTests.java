package com.tradeoption;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class TradeOptionApplicationTests {

    @MockBean
    private com.tradeoption.repository.RocksDBRepository rocksDBRepository;

    @Test
    void contextLoads() {
    }

}
