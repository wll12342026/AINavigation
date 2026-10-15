package com.campus.freshman.analytics;

import com.campus.freshman.ai.AiQueryLog;
import com.campus.freshman.ai.AiQueryLogRepository;
import com.campus.freshman.place.Place;
import com.campus.freshman.place.PlaceRepository;
import com.campus.freshman.task.StudentTask;
import com.campus.freshman.task.StudentTaskRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AiQueryLogRepository queries;
    private final StudentTaskRepository tasks;
    private final PlaceRepository places;

    public AnalyticsController(AiQueryLogRepository queries, StudentTaskRepository tasks, PlaceRepository places) {
        this.queries = queries; this.tasks = tasks; this.places = places;
    }

    @GetMapping("/overview")
    public AnalyticsOverview overview(@RequestParam(defaultValue = "7") int days) {
        int safeDays = Math.max(1, Math.min(days, 30));
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(safeDays);
        LocalDateTime previousStart = start.minusDays(safeDays);
        long queryCount = queries.countByCreatedAtBetween(start, end);
        long previousCount = queries.countByCreatedAtBetween(previousStart, start);
        long successful = queries.countBySuccessfulTrueAndCreatedAtBetween(start, end);
        long navigationPlans = queries.countByIntentAndCreatedAtBetween("PLACE_NAVIGATION", start, end);
        double answerRate = queryCount == 0 ? 0 : successful * 100.0 / queryCount;

        List<StudentTask> taskList = tasks.findAllByDemoDataFalseOrderBySortOrderAsc();
        int completed = taskList.stream().map(StudentTask::getCompletedCount).filter(v -> v != null).mapToInt(Integer::intValue).sum();
        int total = taskList.stream().map(StudentTask::getTotalCount).filter(v -> v != null).mapToInt(Integer::intValue).sum();
        double taskRate = total == 0 ? 0 : completed * 100.0 / total;

        List<AiQueryLog> logs = queries.findAllByCreatedAtBetweenOrderByCreatedAtAsc(start, end);
        Map<LocalDate, Long> perDay = new LinkedHashMap<>();
        for (int i = safeDays - 1; i >= 0; i--) perDay.put(LocalDate.now().minusDays(i), 0L);
        logs.forEach(log -> perDay.computeIfPresent(log.getCreatedAt().toLocalDate(), (date, value) -> value + 1));
        List<TrendPoint> trend = perDay.entrySet().stream()
                .map(entry -> new TrendPoint(entry.getKey().format(DateTimeFormatter.ofPattern("MM-dd")), entry.getValue()))
                .toList();

        Map<String, Long> categories = new LinkedHashMap<>();
        logs.forEach(log -> categories.merge(log.getIntent() == null ? "UNKNOWN" : log.getIntent(), 1L, Long::sum));
        List<CategoryCount> questionCategories = categories.entrySet().stream()
                .map(entry -> new CategoryCount(entry.getKey(), entry.getValue())).toList();

        Map<String, Long> placeMentions = new LinkedHashMap<>();
        for (Place place : places.findAllByDemoDataFalseOrderByUpdatedAtDesc()) {
            long count = logs.stream().filter(log -> log.getQuestion() != null && log.getQuestion().contains(place.getName())).count();
            if (count > 0) placeMentions.put(place.getName(), count);
        }
        List<PopularPlace> popularPlaces = placeMentions.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())).limit(5)
                .map(entry -> new PopularPlace(entry.getKey(), entry.getValue())).toList();

        return new AnalyticsOverview(queryCount, changeRate(queryCount, previousCount), navigationPlans,
                round(taskRate), round(answerRate), completed, total, trend, questionCategories,
                popularPlaces, buildInsights(queryCount, popularPlaces, taskRate));
    }

    private List<String> buildInsights(long queryCount, List<PopularPlace> places, double taskRate) {
        List<String> result = new ArrayList<>();
        if (queryCount == 0) result.add("当前周期暂无AI咨询记录，产生真实咨询后将自动形成趋势。");
        else result.add("当前周期共记录 " + queryCount + " 次AI咨询。");
        if (!places.isEmpty()) result.add("咨询中提及最多的地点是“" + places.getFirst().name() + "”。");
        if (taskRate > 0) result.add("当前任务整体完成率为 " + round(taskRate) + "% 。");
        return result;
    }

    private double changeRate(long current, long previous) {
        if (previous == 0) return current == 0 ? 0 : 100;
        return round((current - previous) * 100.0 / previous);
    }

    private double round(double value) { return Math.round(value * 10.0) / 10.0; }

    public record AnalyticsOverview(long aiQueries, double aiQueryChangeRate, long navigationPlans,
                                    double taskCompletionRate, double aiAnswerRate, int completedTasks,
                                    int totalTasks, List<TrendPoint> trend, List<CategoryCount> categories,
                                    List<PopularPlace> popularPlaces, List<String> insights) {}
    public record TrendPoint(String label, long value) {}
    public record CategoryCount(String category, long count) {}
    public record PopularPlace(String name, long count) {}
}
