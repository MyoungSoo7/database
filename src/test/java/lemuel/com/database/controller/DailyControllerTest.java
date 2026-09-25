package lemuel.com.database.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class DailyControllerTest {

    @Autowired
    WebApplicationContext wac;

    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void problemHasNoSolution() throws Exception {
        mockMvc.perform(get("/api/daily/2026-09-26"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.problemId").value("basic-001"))
            .andExpect(jsonPath("$.url").value("https://database.lemuel.co.kr/problems/basic-001"))
            .andExpect(jsonPath("$.solution").doesNotExist());
    }

    @Test
    void solutionEndpointIncludesSolution() throws Exception {
        mockMvc.perform(get("/api/daily/2026-09-26/solution"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.solution").value("SELECT name, department FROM employees"));
    }

    @Test
    void todayWorks() throws Exception {
        mockMvc.perform(get("/api/daily/today"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.problemId").isNotEmpty());
    }
}
