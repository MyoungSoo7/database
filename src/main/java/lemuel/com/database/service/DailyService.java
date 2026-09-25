package lemuel.com.database.service;

import lemuel.com.database.model.Category;
import lemuel.com.database.model.Problem;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 오늘의 문제. 날짜만으로 정해져서 아침(문제)·저녁(풀이) 호출이 같은 문제를 본다.
 * 카테고리 순서 → id 순으로 한 바퀴 돌고 다시 처음부터. 문제가 늘면 그날부터 순서가 밀린다.
 */
@Service
public class DailyService {

    /** 이 날이 순서의 첫 문제. */
    static final LocalDate START = LocalDate.of(2026, 9, 26);

    private final ProblemService problemService;
    private final List<Problem> rotation;

    public DailyService(ProblemService problemService) {
        this.problemService = problemService;
        Map<String, Integer> order = problemService.getCategories().stream()
            .collect(Collectors.toMap(Category::id, Category::order));
        this.rotation = problemService.getAllProblems().stream()
            .sorted(Comparator.comparing((Problem p) -> order.getOrDefault(p.category(), Integer.MAX_VALUE))
                .thenComparing(Problem::id))
            .toList();
    }

    public Problem forDate(LocalDate date) {
        int n = rotation.size();
        long days = ChronoUnit.DAYS.between(START, date);
        return rotation.get((int) Math.floorMod(days, (long) n));
    }

    List<Problem> rotation() {
        return rotation;
    }

    public String categoryName(String categoryId) {
        return problemService.getCategories().stream()
            .filter(c -> c.id().equals(categoryId))
            .map(Category::name)
            .findFirst().orElse(categoryId);
    }
}
