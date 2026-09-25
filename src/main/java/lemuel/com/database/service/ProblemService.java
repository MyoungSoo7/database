package lemuel.com.database.service;

import lemuel.com.database.model.Category;
import lemuel.com.database.model.Expected;
import lemuel.com.database.model.Problem;
import lemuel.com.database.model.Tuning;
import lemuel.com.database.model.Validation;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProblemService {

    private final List<Category> categories;
    private final List<Problem> problems;

    public ProblemService() {
        this.categories = loadCategories();
        this.problems = loadAllProblems();
    }

    @SuppressWarnings("unchecked")
    private List<Category> loadCategories() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource("classpath:problems/categories.yaml");
            Yaml yaml = new Yaml();
            try (InputStream is = resource.getInputStream()) {
                Map<String, Object> data = yaml.load(is);
                List<Map<String, Object>> cats = (List<Map<String, Object>>) data.get("categories");
                List<Category> result = new ArrayList<>();
                for (Map<String, Object> cat : cats) {
                    result.add(new Category(
                        (String) cat.get("id"),
                        (String) cat.get("name"),
                        (Integer) cat.get("order")
                    ));
                }
                return Collections.unmodifiableList(result);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load categories.yaml", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Problem> loadAllProblems() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:problems/**/*.yaml");
            Yaml yaml = new Yaml();
            List<Problem> result = new ArrayList<>();
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename != null && filename.equals("categories.yaml")) {
                    continue;
                }
                try (InputStream is = resource.getInputStream()) {
                    Map<String, Object> data = yaml.load(is);
                    if (data == null || !data.containsKey("id")) {
                        continue;
                    }
                    result.add(mapToProblem(data));
                }
            }
            return Collections.unmodifiableList(result);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load problems", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Problem mapToProblem(Map<String, Object> data) {
        String id = (String) data.get("id");
        String title = (String) data.get("title");
        String category = (String) data.get("category");
        int difficulty = (Integer) data.getOrDefault("difficulty", 1);
        String description = (String) data.getOrDefault("description", "");
        String type = (String) data.getOrDefault("type", "SELECT");
        String schema = (String) data.getOrDefault("schema", "");
        String hint = (String) data.getOrDefault("hint", "");

        Expected expected = null;
        if (data.containsKey("expected")) {
            Map<String, Object> expMap = (Map<String, Object>) data.get("expected");
            List<String> columns = (List<String>) expMap.getOrDefault("columns", List.of());
            List<List<Object>> rows = (List<List<Object>>) expMap.getOrDefault("rows", List.of());
            boolean orderMatters = (Boolean) expMap.getOrDefault("orderMatters", false);
            expected = new Expected(columns, rows, orderMatters);
        }

        Validation validation = null;
        if (data.containsKey("validation")) {
            Map<String, Object> valMap = (Map<String, Object>) data.get("validation");
            String valType = (String) valMap.get("type");
            Map<String, String> checkPerProfile = (Map<String, String>) valMap.getOrDefault("checkPerProfile", Map.of());
            int expectedValue = (Integer) valMap.getOrDefault("expectedValue", 0);
            validation = new Validation(valType, checkPerProfile, expectedValue);
        }

        Tuning tuning = null;
        if (data.containsKey("tuning")) {
            Map<String, Object> t = (Map<String, Object>) data.get("tuning");
            tuning = new Tuning(
                (String) t.getOrDefault("mode", "INDEX"),
                (String) t.get("table"),
                (Integer) t.getOrDefault("maxRows", 0),
                (List<String>) t.getOrDefault("forbid", List.of()),
                (List<String>) t.getOrDefault("require", List.of()),
                (String) t.get("requireKey"));
        }
        String target = (String) data.get("target");
        String solution = (String) data.get("solution");

        return new Problem(id, title, category, difficulty, description, type, schema, expected, validation, hint,
            target, tuning, solution);
    }

    public List<Category> getCategories() {
        return categories;
    }

    public List<Problem> getAllProblems() {
        return problems;
    }

    public Optional<Problem> getProblemById(String id) {
        return problems.stream()
            .filter(p -> p.id().equals(id))
            .findFirst();
    }

    public List<Problem> getProblemsByCategory(String category) {
        return problems.stream()
            .filter(p -> p.category().equals(category))
            .toList();
    }

    public String getSchemaContent(String problemId) {
        Optional<Problem> problem = getProblemById(problemId);
        if (problem.isEmpty()) {
            throw new IllegalArgumentException("Problem not found: " + problemId);
        }
        String schemaFile = problem.get().schema();
        if (schemaFile == null || schemaFile.isBlank()) {
            return "";
        }
        String category = problem.get().category();
        String schemaPath = "classpath:problems/" + category + "/" + schemaFile;
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource(schemaPath);
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load schema: " + schemaPath, e);
        }
    }
}
