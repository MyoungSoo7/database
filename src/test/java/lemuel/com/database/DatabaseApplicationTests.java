package lemuel.com.database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class DatabaseApplicationTests {

    @Autowired
    WebApplicationContext wac;

    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void fullFlowIntegrationTest() throws Exception {
        // 1. GET /problems → contains "모든 직원 조회"
        mockMvc.perform(get("/problems"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("모든 직원 조회")));

        // 2. GET /problems/basic-001 → contains "employees"
        mockMvc.perform(get("/problems/basic-001"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("employees")));

        String executeBody = """
            {"problemId": "basic-001", "sql": "SELECT name, department FROM employees"}
            """;

        // 3. POST /api/execute → message=OK, rows.length=3
        mockMvc.perform(post("/api/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(executeBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("OK"))
            .andExpect(jsonPath("$.rows.length()").value(3));

        String correctBody = """
            {"problemId": "basic-001", "sql": "SELECT name, department FROM employees"}
            """;

        // 4. POST /api/submit correct → correct=true
        mockMvc.perform(post("/api/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(correctBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correct").value(true));

        String wrongBody = """
            {"problemId": "basic-001", "sql": "SELECT name FROM employees"}
            """;

        // 5. POST /api/submit wrong → correct=false
        mockMvc.perform(post("/api/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(wrongBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correct").value(false));
    }
}
