package com.campus.freshman.dashboard;

import com.campus.freshman.knowledge.KnowledgeItemRepository;
import com.campus.freshman.place.PlaceRepository;
import com.campus.freshman.road.RoadSegment;
import com.campus.freshman.road.RoadSegmentRepository;
import com.campus.freshman.task.StudentTask;
import com.campus.freshman.task.StudentTaskRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final PlaceRepository places;
    private final RoadSegmentRepository roads;
    private final StudentTaskRepository tasks;
    private final KnowledgeItemRepository knowledge;

    public DashboardController(PlaceRepository places, RoadSegmentRepository roads,
                               StudentTaskRepository tasks, KnowledgeItemRepository knowledge) {
        this.places = places;
        this.roads = roads;
        this.tasks = tasks;
        this.knowledge = knowledge;
    }

    @GetMapping("/overview")
    public Overview overview() {
        List<RoadSegment> roadList = roads.findAll();
        int totalRoads = roadList.size();
        long openRoads = roads.countByStatus("OPEN");
        double roadKm = roadList.stream().mapToInt(RoadSegment::getLengthMeters).sum() / 1000.0;
        int openRate = totalRoads == 0 ? 100 : (int) Math.round(openRoads * 100.0 / totalRoads);
        long expiring = knowledge.countByExpiresAtBefore(LocalDate.now().plusDays(30));
        List<TaskProgress> taskProgress = tasks.findAllByOrderBySortOrderAsc().stream()
                .map(task -> new TaskProgress(task.getName(), task.getProgress(), task.getCompletedCount(), task.getTotalCount()))
                .toList();

        return new Overview(
                places.count(),
                BigDecimal.valueOf(roadKm).setScale(1, RoundingMode.HALF_UP),
                openRate,
                knowledge.countByStatus("PENDING"),
                Math.max(0, 99 - (int) expiring),
                expiring,
                taskProgress,
                List.of(
                        new OperationEvent("路网检查完成", "已识别封闭路段及周边可通行道路", "14:23"),
                        new OperationEvent("任务影响已分析", "共影响 3 条新生任务路线", "14:23"),
                        new OperationEvent("新路线已验证", "绕行方案已通过路径仿真", "14:24")
                )
        );
    }

    public record Overview(long placeCount, BigDecimal roadKilometers, int openRoadRate,
                           long pendingKnowledge, int knowledgeHealth, long expiringKnowledge,
                           List<TaskProgress> tasks, List<OperationEvent> events) {}
    public record TaskProgress(String name, Integer progress, Integer completed, Integer total) {}
    public record OperationEvent(String title, String detail, String time) {}
}
