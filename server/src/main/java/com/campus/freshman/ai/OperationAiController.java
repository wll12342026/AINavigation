package com.campus.freshman.ai;

import com.campus.freshman.knowledge.KnowledgeItem;
import com.campus.freshman.knowledge.KnowledgeItemRepository;
import com.campus.freshman.navigation.NavigationService;
import com.campus.freshman.place.Place;
import com.campus.freshman.place.PlaceRepository;
import com.campus.freshman.road.RoadSegment;
import com.campus.freshman.road.RoadSegmentRepository;
import com.campus.freshman.task.StudentTask;
import com.campus.freshman.task.StudentTaskRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

@RestController
@RequestMapping("/api/ai")
public class OperationAiController {
    private final PlaceRepository places;
    private final RoadSegmentRepository roads;
    private final StudentTaskRepository tasks;
    private final KnowledgeItemRepository knowledge;
    private final AiQueryLogRepository logs;
    private final NavigationService navigation;
    private final KnowledgeSearch search;

    public OperationAiController(PlaceRepository places, RoadSegmentRepository roads,
                                 StudentTaskRepository tasks, KnowledgeItemRepository knowledge,
                                 AiQueryLogRepository logs, NavigationService navigation, KnowledgeSearch search) {
        this.places = places; this.roads = roads; this.tasks = tasks;
        this.knowledge = knowledge; this.logs = logs; this.navigation = navigation;
        this.search = search;
    }

    @PostMapping("/analyze")
    public AnalysisResponse analyze(@Valid @RequestBody AnalysisRequest request) {
        String prompt = request.prompt().trim();
        String intent = classify(prompt);
        if ("PLACE_NAVIGATION".equals(intent)) {
            try {
                NavigationService.RouteResponse route = navigation.planFromPrompt(
                        new NavigationService.AiRouteRequest(prompt, null));
                List<String> objects = route.steps().stream().map(NavigationService.RouteStep::roadName).distinct().toList();
                AnalysisResponse routeResponse = new AnalysisResponse(
                        intent,
                        "已生成从“" + route.origin() + "”到“" + route.destination() + "”的可信路线",
                        "全程 " + route.distanceMeters() + " 米，预计 " + route.estimatedMinutes()
                                + " 分钟。" + String.join("；", route.evidence()),
                        route.confidence(),
                        List.of("识别起点与终点", "读取MySQL实时路网", "排除封闭与受限道路", "执行最短路径计算"),
                        objects, List.of(), route, "RULE_BASED"
                );
                saveLog(prompt, routeResponse);
                return routeResponse;
            } catch (ResponseStatusException exception) {
                if (prompt.contains("从") || prompt.contains("怎么去") || prompt.contains("导航")) {
                    AnalysisResponse clarification = new AnalysisResponse(intent, "需要确认导航信息",
                            exception.getReason(), 0, List.of("解析导航需求", "规划未完成：" + exception.getReason()), List.of());
                    saveLog(prompt, clarification);
                    return clarification;
                }
            }
        }
        List<Place> matchedPlaces = places.findAllByDemoDataFalseOrderByUpdatedAtDesc().stream()
                .filter(item -> mentions(prompt, item.getName()) || (item.getAliases() != null &&
                        Arrays.stream(item.getAliases().split("[,，;；、\\n]")).anyMatch(alias -> mentions(prompt, alias.trim())))).limit(5).toList();
        List<RoadSegment> matchedRoads = roads.findAllByDemoDataFalseOrderByUpdatedAtDesc().stream().filter(item -> mentions(prompt, item.getName())).limit(5).toList();
        List<StudentTask> matchedTasks = tasks.findAllByDemoDataFalseOrderBySortOrderAsc().stream()
                .filter(item -> mentions(prompt, item.getName()) || mentions(prompt, item.getLocation())).limit(5).toList();
        List<KnowledgeItem> matchedKnowledge = search.search(prompt,
                knowledge.findAllByDemoDataFalseOrderByUpdatedAtDesc(), LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")));

        List<String> affected = new ArrayList<>();
        matchedRoads.forEach(item -> affected.add(item.getName()));
        matchedTasks.forEach(item -> affected.add(item.getName()));
        matchedPlaces.forEach(item -> affected.add(item.getName()));

        AnalysisResponse response;
        if (affected.isEmpty() && matchedKnowledge.isEmpty()) {
            response = new AnalysisResponse(
                    intent,
                    "数据库中没有找到可核验的相关对象",
                    "本次没有使用前端演示答案。请先在地点、路网、任务或知识库中录入对应信息后重试。",
                    0.35,
                    List.of("读取MySQL地点数据", "读取MySQL路网与任务", "检索已发布知识", "未发现可验证证据"),
                    List.of()
            );
        } else {
            String summary = buildSummary(matchedPlaces, matchedRoads, matchedTasks, matchedKnowledge);
            response = new AnalysisResponse(
                    intent,
                    "已基于数据库核验 " + affected.size() + " 个关联对象",
                    summary,
                    Math.min(0.97, 0.70 + (affected.size() + matchedKnowledge.size()) * 0.04),
                    List.of("检索校园地点", "检查当前路网状态", "匹配新生任务", "核对知识来源"),
                    affected.stream().distinct().toList(), matchedKnowledge.stream().map(item -> new KnowledgeEvidence(
                            item.getId(), item.getTitle(), item.getContent(), item.getSource(), item.getUpdatedAt(), item.getExpiresAt())).toList(),
                    null, "RULE_BASED"
            );
        }
        saveLog(prompt, response);
        return response;
    }

    private String buildSummary(List<Place> placeList, List<RoadSegment> roadList,
                                List<StudentTask> taskList, List<KnowledgeItem> knowledgeList) {
        List<String> facts = new ArrayList<>();
        if (!placeList.isEmpty()) {
            Place place = placeList.getFirst();
            facts.add("地点“" + place.getName() + "”当前状态为 " + place.getStatus());
            if (place.getOpeningHours() != null && !place.getOpeningHours().isBlank()) facts.add("开放时间：" + place.getOpeningHours());
            if (place.getEntranceHint() != null && !place.getEntranceHint().isBlank()) facts.add("入口：" + place.getEntranceHint());
        }
        if (!roadList.isEmpty()) {
            RoadSegment road = roadList.getFirst();
            facts.add("路段“" + road.getName() + "”当前状态为 " + road.getStatus());
        }
        if (!taskList.isEmpty()) {
            StudentTask task = taskList.getFirst();
            facts.add("任务“" + task.getName() + "”办理地点为 " + task.getLocation());
        }
        knowledgeList.forEach(item -> facts.add("《" + item.getTitle() + "》：" + item.getContent()
                + "（来源：" + (item.getSource() == null || item.getSource().isBlank() ? "来源待补充" : item.getSource()) + "）"));
        return String.join("；", facts) + "。结果来自当前MySQL数据，未找到的数据不会自动编造。";
    }

    private boolean mentions(String prompt, String value) {
        String name = value == null ? "" : value.replaceFirst("^【[^】]+】\\s*", "").trim();
        return name.length() >= 2 && prompt.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT));
    }

    private String classify(String prompt) {
        if (prompt.contains("在哪") || prompt.contains("怎么去") || prompt.contains("导航")
                || (prompt.contains("从") && (prompt.contains("去") || prompt.contains("到")))) return "PLACE_NAVIGATION";
        if (prompt.contains("关闭") || prompt.contains("施工") || prompt.contains("封路") || prompt.contains("道路")) return "ROAD_ANALYSIS";
        if (prompt.contains("任务") || prompt.contains("报到") || prompt.contains("材料")) return "TASK_QA";
        return "CAMPUS_DATA_QA";
    }

    private void saveLog(String prompt, AnalysisResponse response) {
        AiQueryLog log = new AiQueryLog();
        log.setQuestion(prompt); log.setIntent(response.intent()); log.setSuccessful(!response.affectedObjects().isEmpty()
                || !response.sources().isEmpty() || response.route() != null);
        log.setConfidence((int) Math.round(response.confidence() * 100)); log.setResponseTitle(response.title());
        logs.save(log);
    }

    public record AnalysisRequest(@NotBlank(message = "分析指令不能为空") String prompt) {}
    public record AnalysisResponse(String intent, String title, String summary, double confidence,
                                   List<String> reasoningSteps, List<String> affectedObjects,
                                   List<KnowledgeEvidence> sources, NavigationService.RouteResponse route, String mode) {
        public AnalysisResponse(String intent, String title, String summary, double confidence,
                                List<String> reasoningSteps, List<String> affectedObjects) {
            this(intent, title, summary, confidence, reasoningSteps, affectedObjects, List.of(), null, "RULE_BASED");
        }
    }
    public record KnowledgeEvidence(Long id, String title, String content, String source,
                                    LocalDateTime updatedAt, LocalDate expiresAt) {}
}
