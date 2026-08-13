package com.example.scientificcalculator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ScientificCalculatorApplicationTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void calculatesAndReadsHistoryThroughRealApplicationContext() throws Exception {
        mvc.perform(post("/api/v1/calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"1+2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(3.0));

        mvc.perform(get("/api/v1/calculations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].expression").value("1+2"))
                .andExpect(jsonPath("$[0].result").value(3.0));
    }
}
