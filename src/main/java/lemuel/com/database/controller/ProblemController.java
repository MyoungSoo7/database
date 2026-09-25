package lemuel.com.database.controller;

import lemuel.com.database.model.Category;
import lemuel.com.database.model.Problem;
import lemuel.com.database.service.ProblemService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/problems")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @GetMapping
    public String problemList(Model model) {
        List<Category> categories = problemService.getCategories();
        Map<String, List<Problem>> problemsByCategory = new LinkedHashMap<>();
        for (Category category : categories) {
            problemsByCategory.put(category.id(), problemService.getProblemsByCategory(category.id()));
        }
        model.addAttribute("categories", categories);
        model.addAttribute("problemsByCategory", problemsByCategory);
        return "problems";
    }

    @GetMapping("/{id}")
    public String problemDetail(@PathVariable String id, Model model) {
        Problem problem = problemService.getProblemById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found: " + id));
        String schemaContent = problemService.getSchemaContent(id);
        model.addAttribute("problem", problem);
        model.addAttribute("schemaContent", schemaContent);
        model.addAttribute("initialSql", initialSql(problem));
        return "problem-detail";
    }

    /** 튜닝 문제는 대상 쿼리를 미리 넣어 둔다. 인덱스 문제는 만들고 바로 EXPLAIN 까지 한 번에 돌리는 틀. */
    static String initialSql(Problem problem) {
        if (problem.target() == null) {
            return "SELECT ";
        }
        if (problem.tuning() != null && "REWRITE".equals(problem.tuning().mode())) {
            return problem.target();
        }
        String table = problem.tuning() != null ? problem.tuning().table() : "orders";
        return "-- 실행: 인덱스를 만든 상태의 실행계획을 봅니다. 제출: CREATE INDEX 만 적용되고 EXPLAIN 은 무시됩니다.\n"
            + "CREATE INDEX idx_ ON " + table + " ();\n"
            + "EXPLAIN " + problem.target();
    }
}
