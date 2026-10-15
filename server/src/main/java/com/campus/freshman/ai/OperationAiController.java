package com.campus.freshman.ai;

import com.campus.freshman.knowledge.KnowledgeItem;
import com.campus.freshman.knowledge.KnowledgeItemRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/ai")
public class OperationAiController {
    private final PlaceRepository places;
    private final RoadSegmentRepository roads;
    private final StudentTaskRepository tasks;
    private final KnowledgeItemRepository knowledge;
    private final AiQueryLogRepository logs;

    public OperationAiController(PlaceRepository places, RoadSegmentRepository roads,
                                 StudentTaskRepository tasks, KnowledgeItemRepository knowledge,
                                 AiQueryLogRepository logs) {
        this.places = places; this.roads = roads; this.tasks = tasks;
        this.knowledge = knowledge; this.logs = logs;
    }

    @PostMapping("/analyze")
    public AnalysisResponse analyze(@Valid @RequestBody AnalysisRequest request) {
        String prompt = request.prompt().trim();
        String intent = classify(prompt);
        List<Place> matchedPlaces = places.findAllByDemoDataFalseOrderByUpdatedAtDesc().stream().filter(item -> mentions(prompt, item.getName())).limit(5).toList();
        List<RoadSegment> matchedRoads = roads.findAllByDemoDataFalseOrderByUpdatedAtDesc().stream().filter(item -> mentions(prompt, item.getName())).limit(5).toList();
        List<StudentTask> matchedTasks = tasks.findAllByDemoDataFalseOrderBySortOrderAsc().stream()
                .filter(item -> mentions(prompt, item.getName()) || mentions(prompt, item.getLocation())).limit(5).toList();
        List<KnowledgeItem> matchedKnowledge = knowledge.findAllByDemoDataFalseOrderByUpdatedAtDesc().stream()
                .filter(item -> mentions(prompt, item.getTitle()) || containsUsefulKeyword(prompt, item.getContent())).limit(3).toList();

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
                    affected.stream().distinct().toList()
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
        }
        if (!roadList.isEmpty()) {
            RoadSegment road = roadList.getFirst();
            facts.add("路段“" + road.getName() + "”当前状态为 " + road.getStatus());
        }
        if (!taskList.isEmpty()) {
            StudentTask task = taskList.getFirst();
            facts.add("任务“" + task.getName() + "”办理地点为 " + task.getLocation());
        }
        if (!knowledgeList.isEmpty()) facts.add("已匹配 " + knowledgeList.size() + " 条知识记录");
        return String.join("；", facts) + "。结果来自当前MySQL数据，未找到的数据不会自动编造。";
    }

    private boolean mentions(String prompt, String value) {
        return value != null && value.length() >= 2 && prompt.toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT));
    }

    private boolean containsUsefulKeyword(String prompt, String content) {
        if (content == null) return false;
        return prompt.chars().filter(ch -> ch > 127).mapToObj(ch -> String.valueOf((char) ch))
                .anyMatch(content::contains);
    }

    private String classify(String prompt) {
        if (prompt.contains("关闭") || prompt.contains("施工") || prompt.contains("封路") || prompt.contains("道路")) return "ROAD_ANALYSIS";
        if (prompt.contains("任务") || prompt.contains("报到") || prompt.contains("材料")) return "TASK_QA";
        if (prompt.contains("在哪") || prompt.contains("怎么去") || prompt.contains("导航")) return "PLACE_NAVIGATION";
        return "CAMPUS_DATA_QA";
    }

    private void saveLog(String prompt, AnalysisResponse response) {
        AiQueryLog log = new AiQueryLog();
        log.setQuestion(prompt); log.setIntent(response.intent()); log.setSuccessful(!response.affectedObjects().isEmpty());
        log.setConfidence((int) Math.round(response.confidence() * 100)); log.setResponseTitle(response.title());
        logs.save(log);
    }

    public record AnalysisRequest(@NotBlank(message = "分析指令不能为空") String prompt) {}
    public record AnalysisResponse(String intent, String title, String summary, double confidence,
                                   List<String> reasoningSteps, List<String> affectedObjects) {}
}
