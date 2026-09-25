package lemuel.com.database.controller;

import lemuel.com.database.model.Problem;
import lemuel.com.database.service.DailyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * n8n 이 매일 부르는 오늘의 문제 API. 날짜는 한국 시간 기준.
 * 풀이는 별도 경로 — 아침 메시지에 정답이 섞이지 않도록.
 */
@RestController
@RequestMapping("/api/daily")
public class DailyController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String SITE = "https://database.lemuel.co.kr";

    private final DailyService dailyService;

    public DailyController(DailyService dailyService) {
        this.dailyService = dailyService;
    }

    @GetMapping("/today")
    public Map<String, Object> today() {
        return problem(LocalDate.now(KST), false);
    }

    @GetMapping("/today/solution")
    public Map<String, Object> todaySolution() {
        return problem(LocalDate.now(KST), true);
    }

    @GetMapping("/{date}")
    public Map<String, Object> byDate(@PathVariable LocalDate date) {
        return problem(date, false);
    }

    @GetMapping("/{date}/solution")
    public Map<String, Object> byDateSolution(@PathVariable LocalDate date) {
        return problem(date, true);
    }

    private Map<String, Object> problem(LocalDate date, boolean withSolution) {
        Problem p = dailyService.forDate(date);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("date", date.toString());
        out.put("problemId", p.id());
        out.put("title", p.title());
        out.put("category", dailyService.categoryName(p.category()));
        out.put("difficulty", p.difficulty());
        out.put("type", p.type());
        out.put("description", p.description());
        out.put("target", p.target());
        out.put("url", SITE + "/problems/" + p.id());
        if (withSolution) {
            out.put("hint", p.hint());
            out.put("solution", p.solution() == null ? null : p.solution().strip());
        }
        return out;
    }
}
