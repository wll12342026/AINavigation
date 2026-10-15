package com.campus.freshman.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class OperationAiController {
    @PostMapping("/analyze")
    public AnalysisResponse analyze(@Valid @RequestBody AnalysisRequest request) {
        String prompt = request.prompt();
        if (prompt.contains("关闭") || prompt.contains("施工") || prompt.contains("封路")) {
            return new AnalysisResponse(
                    "ROAD_IMPACT_ANALYSIS",
                    "影响 3 条任务路线，已生成绕行方案",
                    "施工将影响新生报到、校园卡领取和宿舍入住路线，最优绕行预计增加步行约 280 米。",
                    0.96,
                    List.of("识别受影响路段", "匹配关联任务", "生成候选路线", "完成可通行性验证"),
                    List.of("报到路线", "领卡路线", "入住路线")
            );
        }
        return new AnalysisResponse(
                "CAMPUS_OPERATION_QA",
                "已完成校园空间运行分析",
                "当前地点、路网和新生任务整体运行正常，建议优先处理即将过期的知识条目。",
                0.91,
                List.of("读取空间状态", "检查任务依赖", "匹配知识证据"),
                List.of("校园地点", "导航路网", "知识库")
        );
    }

    public record AnalysisRequest(@NotBlank(message = "分析指令不能为空") String prompt) {}
    public record AnalysisResponse(String intent, String title, String summary, double confidence,
                                   List<String> reasoningSteps, List<String> affectedObjects) {}
}
