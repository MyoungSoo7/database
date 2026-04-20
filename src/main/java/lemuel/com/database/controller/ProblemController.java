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
        return "problem-detail";
    }
}
