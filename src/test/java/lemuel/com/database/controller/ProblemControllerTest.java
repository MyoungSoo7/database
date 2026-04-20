package lemuel.com.database.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ProblemControllerTest {

    @Autowired
    WebApplicationContext wac;

    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void problemListPage() throws Exception {
        mockMvc.perform(get("/problems"))
            .andExpect(status().isOk())
            .andExpect(view().name("problems"))
            .andExpect(model().attributeExists("categories"))
            .andExpect(model().attributeExists("problemsByCategory"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("SQL 코딩 테스트")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("모든 직원 조회")));
    }

    @Test
    void problemDetailPage() throws Exception {
        mockMvc.perform(get("/problems/basic-001"))
            .andExpect(status().isOk())
            .andExpect(view().name("problem-detail"))
            .andExpect(model().attributeExists("problem"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("codemirror")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("employees")));
    }

    @Test
    void problemNotFound() throws Exception {
        mockMvc.perform(get("/problems/nonexistent"))
            .andExpect(status().isNotFound());
    }
}
