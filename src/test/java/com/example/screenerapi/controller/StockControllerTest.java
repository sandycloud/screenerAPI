package com.example.screenerapi.controller;

import com.example.screenerapi.entity.StockInfo;
import com.example.screenerapi.entity.StockPrice5Min;
import com.example.screenerapi.repository.StockInfoRepository;
import com.example.screenerapi.repository.StockPrice5MinRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class StockControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StockInfoRepository stockInfoRepository;
    @Autowired
    private StockPrice5MinRepository stockPrice5MinRepository;

    @Test
    void testFetchAndStoreStockInfo() throws Exception {
        String payload = "{" +
                "\"data\": {" +
                "\"sort\":\"Mcap\",\"sorder\":\"desc\",\"count\":250," +
                "\"params\":[{" +
                "\"field\":\"FnoFlag\",\"op\":\"\",\"val\":\"1\"},{\"field\":\"OgInst\",\"op\":\"\",\"val\":\"ES\"}]," +
                "\"fields\":[\"Isin\",\"DispSym\",\"volume\",\"Pb\"],\"pgno\":1}}";
        mockMvc.perform(MockMvcRequestBuilders.post("/api/stockinfo/fetch-and-store")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());
    }

    //@Test
    void testGetAllStockInfo() throws Exception {
        stockInfoRepository.save(new StockInfo() {{ setIsin("TESTISIN"); setName("Test Name"); }});
        mockMvc.perform(MockMvcRequestBuilders.get("/api/stockinfo/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    //@Test
    void testGetByIsin() throws Exception {
        StockInfo info = new StockInfo();
        info.setIsin("TESTISIN2");
        info.setName("Test Name 2");
        stockInfoRepository.save(info);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/stockinfo/by-isin/TESTISIN2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isin", is("TESTISIN2")));
    }

    //@Test
    void testSearchByName() throws Exception {
        StockInfo info = new StockInfo();
        info.setIsin("TESTISIN3");
        info.setName("Alpha Beta");
        stockInfoRepository.save(info);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/stockinfo/search?name=Alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", containsStringIgnoringCase("Alpha")));
    }

    // Add similar tests for StockController endpoints as needed
}
