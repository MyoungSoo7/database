package lemuel.com.database.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SqlExecuteControllerTest {

    @Autowired
    WebApplicationContext wac;

    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void executeReturnsResult() throws Exception {
        String body = """
            {"problemId": "basic-001", "sql": "SELECT name, department FROM employees"}
            """;
        mockMvc.perform(post("/api/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("OK"))
            .andExpect(jsonPath("$.columns").isArray())
            .andExpect(jsonPath("$.rows").isArray())
            .andExpect(jsonPath("$.rows.length()").value(3));
    }

    @Test
    void submitReturnsGradingResult() throws Exception {
        String body = """
            {"problemId": "basic-001", "sql": "SELECT name, department FROM employees"}
            """;
        mockMvc.perform(post("/api/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correct").value(true));
    }

    @Test
    void submitWrongAnswer() throws Exception {
        String body = """
            {"problemId": "basic-001", "sql": "SELECT name FROM employees"}
            """;
        mockMvc.perform(post("/api/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correct").value(false));
    }
}
