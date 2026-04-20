package lemuel.com.database.service;

import lemuel.com.database.model.Category;
import lemuel.com.database.model.Problem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("h2")
class ProblemServiceTest {

    @Autowired
    private ProblemService problemService;

    @Test
    void loadCategories() {
        List<Category> categories = problemService.getCategories();
        assertThat(categories).isNotEmpty();
        assertThat(categories.get(0).id()).isEqualTo("basic");
    }

    @Test
    void loadAllProblems() {
        List<Problem> problems = problemService.getAllProblems();
        assertThat(problems).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void findProblemById() {
        Optional<Problem> problem = problemService.getProblemById("basic-001");
        assertThat(problem).isPresent();
        assertThat(problem.get().title()).isEqualTo("모든 직원 조회");
        assertThat(problem.get().category()).isEqualTo("basic");
        assertThat(problem.get().expected().columns()).containsExactly("name", "department");
    }

    @Test
    void findProblemsByCategory() {
        List<Problem> problems = problemService.getProblemsByCategory("basic");
        assertThat(problems).hasSizeGreaterThanOrEqualTo(2);
        assertThat(problems).allMatch(p -> p.category().equals("basic"));
    }

    @Test
    void getSchemaContent() {
        String schema = problemService.getSchemaContent("basic-001");
        assertThat(schema).containsIgnoringCase("CREATE TABLE");
        assertThat(schema).containsIgnoringCase("employees");
    }
}
