package lemuel.com.database.service;

import lemuel.com.database.model.Problem;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DailyServiceTest {

    private final ProblemService problems = new ProblemService();
    private final DailyService daily = new DailyService(problems);

    @Test
    void startsWithFirstBasicProblem() {
        assertThat(daily.forDate(DailyService.START).id()).isEqualTo("basic-001");
    }

    @Test
    void sameDateGivesSameProblem() {
        LocalDate d = LocalDate.of(2026, 10, 3);
        assertThat(daily.forDate(d)).isEqualTo(daily.forDate(d));
    }

    @Test
    void oneCycleVisitsEveryProblemOnce() {
        int n = problems.getAllProblems().size();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < n; i++) {
            seen.add(daily.forDate(DailyService.START.plusDays(i)).id());
        }
        assertThat(seen).hasSize(n);
        assertThat(daily.forDate(DailyService.START.plusDays(n)).id()).isEqualTo("basic-001");
        assertThat(daily.forDate(DailyService.START.minusDays(1)).id())
            .isEqualTo(daily.rotation().get(n - 1).id());
    }

    /** 오늘의 풀이가 빈 칸으로 나가지 않도록 — 모든 문제에 정답 예시가 있어야 한다. */
    @Test
    void everyProblemHasSolution() {
        assertThat(problems.getAllProblems()).allSatisfy((Problem p) ->
            assertThat(p.solution()).as(p.id()).isNotBlank());
    }
}
