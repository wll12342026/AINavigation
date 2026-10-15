package com.campus.freshman.config;

import com.campus.freshman.knowledge.KnowledgeItemRepository;
import com.campus.freshman.place.PlaceRepository;
import com.campus.freshman.road.RoadSegmentRepository;
import com.campus.freshman.task.StudentTaskRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DemoDataClassifier implements CommandLineRunner {
    private static final String DEMO_PLACE_DESCRIPTION = "数字孪生语义地点，等待真实校园模型坐标校准";
    private final PlaceRepository places;
    private final RoadSegmentRepository roads;
    private final StudentTaskRepository tasks;
    private final KnowledgeItemRepository knowledge;

    public DemoDataClassifier(PlaceRepository places, RoadSegmentRepository roads,
                              StudentTaskRepository tasks, KnowledgeItemRepository knowledge) {
        this.places = places; this.roads = roads; this.tasks = tasks; this.knowledge = knowledge;
    }

    @Override
    public void run(String... args) {
        var placeRows = places.findAll().stream().filter(item -> !Boolean.TRUE.equals(item.getDemoData()))
                .filter(item -> DEMO_PLACE_DESCRIPTION.equals(item.getDescription())).toList();
        placeRows.forEach(item -> item.markDemoData()); places.saveAll(placeRows);

        var roadRows = roads.findAll().stream().filter(item -> !Boolean.TRUE.equals(item.getDemoData()))
                .filter(item -> item.getName() != null && (item.getName().matches("校园道路 \\d+") || "图书馆东侧道路".equals(item.getName())))
                .filter(item -> item.getFromNode() != null && item.getFromNode().matches("N\\d+")).toList();
        roadRows.forEach(item -> item.markDemoData()); roads.saveAll(roadRows);

        var taskRows = tasks.findAll().stream().filter(item -> !Boolean.TRUE.equals(item.getDemoData()))
                .filter(item -> "学院报到".equals(item.getName()) || "领取校园卡".equals(item.getName())
                        || "宿舍入住".equals(item.getName()) || "校园导览".equals(item.getName()))
                .filter(item -> item.getTotalCount() != null && item.getTotalCount() == 480).toList();
        taskRows.forEach(item -> item.markDemoData()); tasks.saveAll(taskRows);

        var knowledgeRows = knowledge.findAll().stream().filter(item -> !Boolean.TRUE.equals(item.getDemoData()))
                .filter(item -> item.getTitle() != null && item.getTitle().matches("新生校园知识条目 \\d+"))
                .filter(item -> "2026级新生入学指南".equals(item.getSource())).toList();
        knowledgeRows.forEach(item -> item.markDemoData()); knowledge.saveAll(knowledgeRows);
    }
}
