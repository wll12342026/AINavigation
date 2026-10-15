package com.campus.freshman.dashboard;

import com.campus.freshman.ai.AiQueryLog;
import com.campus.freshman.ai.AiQueryLogRepository;
import com.campus.freshman.common.BaseEntity;
import com.campus.freshman.knowledge.KnowledgeItemRepository;
import com.campus.freshman.knowledge.KnowledgeItem;
import com.campus.freshman.place.Place;
import com.campus.freshman.place.PlaceRepository;
import com.campus.freshman.road.RoadSegment;
import com.campus.freshman.road.RoadSegmentRepository;
import com.campus.freshman.task.StudentTaskRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final PlaceRepository places;
    private final RoadSegmentRepository roads;
    private final StudentTaskRepository tasks;
    private final KnowledgeItemRepository knowledge;
    private final AiQueryLogRepository queries;

    public DashboardController(PlaceRepository places, RoadSegmentRepository roads,
                               StudentTaskRepository tasks, KnowledgeItemRepository knowledge,
                               AiQueryLogRepository queries) {
        this.places = places; this.roads = roads; this.tasks = tasks;
        this.knowledge = knowledge; this.queries = queries;
    }

    @GetMapping("/overview")
    public Overview overview() {
        List<Place> placeList = places.findAllByDemoDataFalseOrderByUpdatedAtDesc();
        List<RoadSegment> roadList = roads.findAllByDemoDataFalseOrderByUpdatedAtDesc();
        int totalRoads = roadList.size();
        long openRoads = roadList.stream().filter(item -> "OPEN".equals(item.getStatus())).count();
        double roadKm = roadList.stream().map(RoadSegment::getLengthMeters).filter(v -> v != null).mapToInt(Integer::intValue).sum() / 1000.0;
        int openRate = totalRoads == 0 ? 0 : (int) Math.round(openRoads * 100.0 / totalRoads);
        List<KnowledgeItem> knowledgeList = knowledge.findAllByDemoDataFalseOrderByUpdatedAtDesc();
        LocalDate today = LocalDate.now();
        long expiring = knowledgeList.stream().filter(item -> item.getExpiresAt() != null)
                .filter(item -> !item.getExpiresAt().isBefore(today) && item.getExpiresAt().isBefore(today.plusDays(30))).count();
        long healthyKnowledge = knowledgeList.stream().filter(item -> "PUBLISHED".equals(item.getStatus()))
                .filter(item -> item.getContent() != null && !item.getContent().isBlank())
                .filter(item -> item.getSource() != null && !item.getSource().isBlank())
                .filter(item -> item.getExpiresAt() == null || !item.getExpiresAt().isBefore(today)).count();
        int knowledgeHealth = knowledgeList.isEmpty() ? 0 : (int) Math.round(healthyKnowledge * 100.0 / knowledgeList.size());
        List<TaskProgress> taskProgress = tasks.findAllByDemoDataFalseOrderBySortOrderAsc().stream()
                .map(task -> new TaskProgress(task.getName(), safe(task.getProgress()), safe(task.getCompletedCount()), safe(task.getTotalCount())))
                .toList();
        long excludedDemoRecords = (places.count() - placeList.size()) + (roads.count() - roadList.size())
                + (tasks.count() - taskProgress.size()) + (knowledge.count() - knowledgeList.size());

        long buildingCount = placeList.stream().map(Place::getBuilding).filter(value -> value != null && !value.isBlank()).distinct().count();
        long servicePointCount = placeList.stream().filter(item -> "SERVICE".equals(item.getCategory())).count();
        Set<String> nodes = new LinkedHashSet<>();
        roadList.forEach(road -> { if (road.getFromNode() != null) nodes.add(road.getFromNode()); if (road.getToNode() != null) nodes.add(road.getToNode()); });

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay();
        long consultationsToday = queries.countByCreatedAtBetween(todayStart, tomorrowStart);
        List<TrendPoint> consultationTrend = hourlyTrend(todayStart, tomorrowStart);
        List<RecentUpdate> recentKnowledge = knowledgeList.stream().limit(3)
                .map(item -> new RecentUpdate(item.getTitle(), item.getUpdatedAt(), item.getStatus())).toList();

        return new Overview(
                placeList.size(), buildingCount, servicePointCount,
                BigDecimal.valueOf(roadKm).setScale(1, RoundingMode.HALF_UP), nodes.size(), roadList.size(), openRate,
                knowledge.countByStatusAndDemoDataFalse("PENDING"), knowledgeHealth, expiring, excludedDemoRecords,
                consultationsToday, consultationTrend, taskProgress, dynamicEvents(roadList, taskProgress), recentKnowledge
        );
    }

    private List<TrendPoint> hourlyTrend(LocalDateTime start, LocalDateTime end) {
        List<AiQueryLog> logs = queries.findAllByCreatedAtBetweenOrderByCreatedAtAsc(start, end);
        List<TrendPoint> points = new ArrayList<>();
        for (int hour = 0; hour < 24; hour += 2) {
            int from = hour;
            int to = hour + 2;
            long count = logs.stream().filter(log -> {
                int value = log.getCreatedAt().getHour();
                return value >= from && value < to;
            }).count();
            points.add(new TrendPoint(String.format("%02d:00", hour), count));
        }
        return points;
    }

    private List<OperationEvent> dynamicEvents(List<RoadSegment> roadList, List<TaskProgress> taskList) {
        List<OperationEvent> events = new ArrayList<>();
        roadList.stream().max(Comparator.comparing(BaseEntity::getUpdatedAt)).ifPresent(road ->
                events.add(new OperationEvent("路网数据已更新", road.getName() + " · " + road.getStatus(), time(road.getUpdatedAt()))));
        if (!taskList.isEmpty()) {
            int average = (int) Math.round(taskList.stream().mapToInt(TaskProgress::progress).average().orElse(0));
            events.add(new OperationEvent("任务进度已汇总", taskList.size() + " 个流程，平均完成率 " + average + "%", time(LocalDateTime.now())));
        }
        knowledge.findAllByDemoDataFalseOrderByUpdatedAtDesc().stream().findFirst().ifPresent(item ->
                events.add(new OperationEvent("知识库已更新", item.getTitle(), time(item.getUpdatedAt()))));
        return events;
    }

    private String time(LocalDateTime value) { return value == null ? "--:--" : value.format(DateTimeFormatter.ofPattern("HH:mm")); }
    private int safe(Integer value) { return value == null ? 0 : value; }

    public record Overview(long placeCount, long buildingCount, long servicePointCount,
                           BigDecimal roadKilometers, int roadNodeCount, int roadCount, int openRoadRate,
                           long pendingKnowledge, int knowledgeHealth, long expiringKnowledge,
                           long excludedDemoRecords, long consultationsToday, List<TrendPoint> consultationTrend,
                           List<TaskProgress> tasks, List<OperationEvent> events,
                           List<RecentUpdate> recentKnowledge) {}
    public record TrendPoint(String label, long value) {}
    public record TaskProgress(String name, int progress, int completed, int total) {}
    public record OperationEvent(String title, String detail, String time) {}
    public record RecentUpdate(String title, LocalDateTime updatedAt, String status) {}
}
