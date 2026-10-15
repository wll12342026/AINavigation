package com.campus.freshman.config;

import com.campus.freshman.knowledge.KnowledgeItem;
import com.campus.freshman.knowledge.KnowledgeItemRepository;
import com.campus.freshman.place.Place;
import com.campus.freshman.place.PlaceRepository;
import com.campus.freshman.road.RoadSegment;
import com.campus.freshman.road.RoadSegmentRepository;
import com.campus.freshman.task.StudentTask;
import com.campus.freshman.task.StudentTaskRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataSeeder {
    @Bean
    @ConditionalOnProperty(name = "campus.seed-demo-data", havingValue = "true")
    CommandLineRunner seed(PlaceRepository places, RoadSegmentRepository roads,
                           StudentTaskRepository tasks, KnowledgeItemRepository knowledge) {
        return args -> {
            seedPlaces(places);
            seedRoads(roads);
            seedTasks(tasks);
            seedKnowledge(knowledge);
        };
    }

    private void seedPlaces(PlaceRepository repository) {
        if (repository.count() > 0) return;
        String[] names = {"东门", "学院报到点", "校园卡中心", "学生公寓A区", "图书馆", "校医院", "第一食堂", "体育馆"};
        String[] categories = {"GATE", "SERVICE", "SERVICE", "DORMITORY", "LIBRARY", "MEDICAL", "DINING", "SPORT"};
        List<Place> data = new ArrayList<>();
        for (int i = 0; i < 128; i++) {
            Place place = new Place();
            String base = names[i % names.length];
            place.setName(i < names.length ? base : base + "-扩展点" + (i + 1));
            place.setCategory(categories[i % categories.length]);
            place.setBuilding("校园建筑 " + (i % 36 + 1));
            place.setFloor((i % 5 + 1) + "F");
            place.setStatus(i % 31 == 0 ? "MAINTENANCE" : "OPEN");
            place.setLongitude(116.3000 + i * 0.00008);
            place.setLatitude(39.9000 + i * 0.00005);
            place.setEntranceHint("建议从东侧主入口进入");
            place.setDescription("数字孪生语义地点，等待真实校园模型坐标校准");
            place.markDemoData();
            data.add(place);
        }
        repository.saveAll(data);
    }

    private void seedRoads(RoadSegmentRepository repository) {
        if (repository.count() > 0) return;
        List<RoadSegment> data = new ArrayList<>();
        for (int i = 1; i <= 94; i++) {
            RoadSegment road = new RoadSegment();
            road.setName(i == 18 ? "图书馆东侧道路" : "校园道路 " + i);
            road.setFromNode("N" + i);
            road.setToNode("N" + (i + 1));
            road.setLengthMeters(55 + i % 37);
            road.setStatus(i <= 90 ? "OPEN" : "RESTRICTED");
            road.setRestrictionReason(i <= 90 ? null : "临时施工");
            road.setAccessible(i % 17 != 0);
            road.markDemoData();
            data.add(road);
        }
        repository.saveAll(data);
    }

    private void seedTasks(StudentTaskRepository repository) {
        if (repository.count() > 0) return;
        repository.saveAll(List.of(
                task("学院报到", "身份验证与入学资料审核", "综合楼 A201", 89, 428, 480, 20, 1),
                task("领取校园卡", "领取校园身份与消费卡", "综合服务中心 1 楼", 82, 392, 480, 25, 2),
                task("宿舍入住", "登记入住并领取钥匙", "学生公寓 A 区", 74, 356, 480, 35, 3),
                task("校园导览", "熟悉教学与生活服务区域", "校园主环路", 65, 310, 480, 45, 4)
        ));
    }

    private StudentTask task(String name, String description, String location, int progress,
                             int completed, int total, int minutes, int order) {
        StudentTask task = new StudentTask();
        task.setName(name); task.setDescription(description); task.setLocation(location); task.setStatus("ACTIVE");
        task.setProgress(progress); task.setCompletedCount(completed); task.setTotalCount(total);
        task.setEstimatedMinutes(minutes); task.setSortOrder(order);
        task.markDemoData();
        return task;
    }

    private void seedKnowledge(KnowledgeItemRepository repository) {
        if (repository.count() > 0) return;
        List<KnowledgeItem> data = new ArrayList<>();
        for (int i = 1; i <= 24; i++) {
            KnowledgeItem item = new KnowledgeItem();
            item.setTitle("新生校园知识条目 " + i);
            item.setCategory(i % 2 == 0 ? "办事流程" : "空间地点");
            item.setContent("用于校园任务智能体回答和路线决策的可追溯知识内容。");
            item.setStatus(i <= 12 ? "PENDING" : "PUBLISHED");
            item.setSource("2026级新生入学指南");
            item.setExpiresAt(i <= 7 ? LocalDate.now().plusDays(i + 5L) : LocalDate.now().plusMonths(6));
            item.markDemoData();
            data.add(item);
        }
        repository.saveAll(data);
    }
}
