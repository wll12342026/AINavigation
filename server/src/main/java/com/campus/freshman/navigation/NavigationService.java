package com.campus.freshman.navigation;

import com.campus.freshman.place.Place;
import com.campus.freshman.place.PlaceRepository;
import com.campus.freshman.road.RoadSegment;
import com.campus.freshman.road.RoadSegmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

@Service
public class NavigationService {
    private final RoadSegmentRepository roads;
    private final PlaceRepository places;
    private final NavigationPlanRepository plans;

    public NavigationService(RoadSegmentRepository roads, PlaceRepository places, NavigationPlanRepository plans) {
        this.roads = roads;
        this.places = places;
        this.plans = plans;
    }

    public RouteResponse plan(RouteRequest request) {
        String origin = resolveNode(request.origin());
        String destination = resolveNode(request.destination());
        boolean accessibleOnly = Boolean.TRUE.equals(request.accessibleOnly());
        boolean avoidRestricted = request.avoidRestricted() == null || request.avoidRestricted();
        String optimization = request.optimization() == null ? "DISTANCE" : request.optimization();
        if (!Set.of("DISTANCE", "TIME").contains(optimization)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "规划策略只支持 DISTANCE 或 TIME");
        }
        return calculate(origin, destination, accessibleOnly, avoidRestricted, "DATABASE_ROUTE", 1.0, optimization);
    }

    public RouteResponse planFromPrompt(AiRouteRequest request) {
        String prompt = request.prompt() == null ? "" : request.prompt().trim();
        if (prompt.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入导航需求");

        List<PlaceMention> mentions = new ArrayList<>();
        for (Place place : places.findAllByDemoDataFalseOrderByUpdatedAtDesc()) {
            int index = names(place).stream().mapToInt(alias -> indexOfIgnoreCase(prompt, alias))
                    .filter(value -> value >= 0).min().orElse(-1);
            if (index >= 0) mentions.add(new PlaceMention(place.getName(), index));
        }
        mentions.sort(Comparator.comparingInt(PlaceMention::index));
        if (mentions.size() > 2 || (mentions.size() == 2 && mentions.get(0).index() == mentions.get(1).index())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "检测到多个或重名地点，请明确指定一个起点和一个终点；多事项路线尚未启用");
        }

        String origin;
        String destination;
        if (mentions.size() >= 2) {
            origin = mentions.get(0).name();
            destination = mentions.get(mentions.size() - 1).name();
        } else if (mentions.size() == 1 && request.origin() != null && !request.origin().isBlank()) {
            origin = resolveNode(request.origin());
            destination = mentions.getFirst().name();
        } else {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "没有识别出完整的起点和终点，请使用“从东门去图书馆”这样的表达");
        }

        boolean accessibleOnly = containsAny(prompt, "无障碍", "轮椅", "行动不便", "坡道");
        boolean avoidRestricted = containsAny(prompt, "避开", "施工", "封路", "关闭", "绕行") || !prompt.contains("允许受限");
        double confidence = Math.min(0.97, 0.78 + mentions.size() * 0.07 + (accessibleOnly ? 0.04 : 0));
        return calculate(resolveNode(origin), resolveNode(destination), accessibleOnly, avoidRestricted,
                "SEMANTIC_ROUTE", confidence, containsAny(prompt, "最快", "用时最少", "时间最短", "赶时间") ? "TIME" : "DISTANCE");
    }

    private RouteResponse calculate(String origin, String destination, boolean accessibleOnly,
                                    boolean avoidRestricted, String strategy, double confidence, String optimization) {
        places.findAllByDemoDataFalseOrderByUpdatedAtDesc().stream()
                .filter(place -> cleanName(place.getName()).equals(cleanName(destination)))
                .filter(place -> !"OPEN".equals(place.getStatus())).findFirst().ifPresent(place -> {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "目的地“" + place.getName() + "”当前未开放，请先确认开放状态");
                });
        if (origin.equals(destination)) {
            return persist(new RouteResponse(origin, destination, 0, 0, accessibleOnly, avoidRestricted,
                    strategy, confidence, List.of(), List.of("起点与终点相同，无需经过道路"), List.of(), optimization));
        }

        List<RoadSegment> allRoads = roads.findAllByDemoDataFalseOrderByUpdatedAtDesc().stream()
                .sorted(Comparator.comparing(RoadSegment::getName).thenComparing(RoadSegment::getId)).toList();
        Map<String, List<Edge>> graph = new HashMap<>();
        List<String> blocked = new ArrayList<>();
        for (RoadSegment road : allRoads) {
            if (road.getFromNode() == null || road.getToNode() == null
                    || road.getFromNode().isBlank() || road.getToNode().isBlank()
                    || safeLength(road) <= 0 || !Set.of("OPEN", "CLOSED", "RESTRICTED").contains(String.valueOf(road.getStatus()))) continue;
            boolean closed = "CLOSED".equals(road.getStatus());
            boolean restricted = "RESTRICTED".equals(road.getStatus());
            boolean inaccessible = accessibleOnly && !Boolean.TRUE.equals(road.getAccessible());
            if (closed || inaccessible || (avoidRestricted && restricted)) {
                blocked.add(road.getName() + "（" + reason(road, inaccessible) + "）");
                continue;
            }
            int weight = "TIME".equals(optimization) ? travelSeconds(road) : safeLength(road);
            graph.computeIfAbsent(road.getFromNode(), key -> new ArrayList<>())
                    .add(new Edge(road.getToNode(), road, weight));
            graph.computeIfAbsent(road.getToNode(), key -> new ArrayList<>());
            if (!Boolean.FALSE.equals(road.getBidirectional())) {
                graph.get(road.getToNode()).add(new Edge(road.getFromNode(), road, weight));
            }
        }

        if (!graph.containsKey(origin) || !graph.containsKey(destination)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "起点或终点尚未接入当前可用路网");
        }

        Map<String, Integer> distances = new HashMap<>();
        Map<String, Previous> previous = new HashMap<>();
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(Comparator.comparingInt(NodeDistance::distance));
        Set<String> visited = new HashSet<>();
        distances.put(origin, 0);
        queue.add(new NodeDistance(origin, 0));

        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();
            if (!visited.add(current.node())) continue;
            if (current.node().equals(destination)) break;
            for (Edge edge : graph.getOrDefault(current.node(), List.of())) {
                int candidate = current.distance() + edge.weight();
                if (candidate < distances.getOrDefault(edge.target(), Integer.MAX_VALUE)) {
                    distances.put(edge.target(), candidate);
                    previous.put(edge.target(), new Previous(current.node(), edge.road()));
                    queue.add(new NodeDistance(edge.target(), candidate));
                }
            }
        }

        if (!previous.containsKey(destination)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    accessibleOnly ? "当前无障碍路网无法到达该地点" : "当前开放路网中不存在可达路线");
        }

        List<RouteStep> steps = new ArrayList<>();
        String cursor = destination;
        while (!cursor.equals(origin)) {
            Previous item = previous.get(cursor);
            if (item == null) break;
            RoadSegment road = item.road();
            steps.addFirst(new RouteStep(road.getId(), road.getName(), item.node(), cursor,
                    safeLength(road), road.getStatus(), Boolean.TRUE.equals(road.getAccessible()),
                    road.getRestrictionReason()));
            cursor = item.node();
        }

        int distance = steps.stream().mapToInt(RouteStep::lengthMeters).sum();
        int seconds = steps.stream().mapToInt(step -> travelSeconds(allRoads.stream()
                .filter(road -> road.getId().equals(step.roadId())).findFirst().orElseThrow())).sum();
        int minutes = Math.max(1, (int) Math.ceil(seconds / 60.0));
        List<String> evidence = new ArrayList<>();
        evidence.add("路线由当前 MySQL 路网实时计算，共经过 " + steps.size() + " 条道路");
        evidence.add("TIME".equals(optimization) ? "按预计通行时间最少规划" : "按总距离最短规划");
        evidence.add("遵守道路单向限制；时间优先使用录入的路段耗时，未录入按75米/分钟估算，非实时到达时间");
        if (accessibleOnly) evidence.add("已排除所有不支持无障碍通行的道路");
        if (avoidRestricted) evidence.add("已避开关闭、施工或受限道路");
        steps.stream().filter(step -> "RESTRICTED".equals(step.status())).findFirst()
                .ifPresent(step -> evidence.add("路线包含受限路段“" + step.roadName() + "”，预计通行时间增加35%"));

        return persist(new RouteResponse(origin, destination, distance, minutes, accessibleOnly,
                avoidRestricted, strategy, confidence, steps, evidence, blocked.stream().limit(8).toList(), optimization));
    }

    private RouteResponse persist(RouteResponse response) {
        NavigationPlanLog log = new NavigationPlanLog();
        log.setOriginName(response.origin());
        log.setDestinationName(response.destination());
        log.setDistanceMeters(response.distanceMeters());
        log.setEstimatedMinutes(response.estimatedMinutes());
        log.setAccessibleOnly(response.accessibleOnly());
        log.setAvoidRestricted(response.avoidRestricted());
        log.setOptimization(response.optimization());
        log.setStatus("SUCCESS");
        log.setRouteSummary(response.steps().stream().map(RouteStep::roadName).reduce((a, b) -> a + " → " + b).orElse("无需经过道路"));
        plans.save(log);
        return response;
    }

    public List<NavigationPlanLog> history() {
        return plans.findTop20ByOrderByCreatedAtDesc();
    }

    private String resolveNode(String requested) {
        if (requested == null || requested.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "起点和终点不能为空");
        }
        String trimmed = requested.trim();
        Set<String> nodes = new LinkedHashSet<>();
        roads.findAllByDemoDataFalseOrderByUpdatedAtDesc().forEach(road -> {
            if (road.getFromNode() != null) nodes.add(road.getFromNode());
            if (road.getToNode() != null) nodes.add(road.getToNode());
        });
        if (nodes.contains(trimmed)) return trimmed;
        Set<String> matches = new LinkedHashSet<>();
        nodes.stream().filter(node -> cleanName(node).equalsIgnoreCase(cleanName(trimmed))).forEach(matches::add);
        places.findAllByDemoDataFalseOrderByUpdatedAtDesc().stream()
                .filter(place -> names(place).stream().anyMatch(alias -> alias.equalsIgnoreCase(cleanName(trimmed))))
                .forEach(place -> nodes.stream().filter(node -> cleanName(node).equals(cleanName(place.getName()))).forEach(matches::add));
        if (matches.size() > 1) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "地点“" + requested + "”存在重名，请选择完整节点名称");
        return matches.stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "地点“" + requested + "”尚未接入路网"));
    }

    private List<String> names(Place place) {
        List<String> result = new ArrayList<>();
        result.add(cleanName(place.getName()));
        if (place.getAliases() != null) {
            for (String alias : place.getAliases().split("[,，;；、\\n]")) {
                if (!alias.isBlank()) result.add(alias.trim());
            }
        }
        return result;
    }

    private int travelSeconds(RoadSegment road) {
        double seconds = road.getTravelSeconds() != null && road.getTravelSeconds() > 0
                ? road.getTravelSeconds() : safeLength(road) / 1.25;
        return Math.max(1, (int) Math.ceil(seconds * ("RESTRICTED".equals(road.getStatus()) ? 1.35 : 1.0)));
    }

    private String cleanName(String value) {
        if (value == null) return "";
        return value.replaceFirst("^【[^】]+】\\s*", "").trim();
    }

    private int indexOfIgnoreCase(String source, String value) {
        if (value == null || value.length() < 2) return -1;
        return source.toLowerCase(Locale.ROOT).indexOf(value.toLowerCase(Locale.ROOT));
    }

    private boolean containsAny(String prompt, String... values) {
        for (String value : values) if (prompt.contains(value)) return true;
        return false;
    }

    private String reason(RoadSegment road, boolean inaccessible) {
        if (inaccessible) return "不支持无障碍通行";
        if (road.getRestrictionReason() != null && !road.getRestrictionReason().isBlank()) return road.getRestrictionReason();
        return "道路状态为 " + road.getStatus();
    }

    private int safeLength(RoadSegment road) {
        return road.getLengthMeters() == null ? 0 : Math.max(0, road.getLengthMeters());
    }

    private record Edge(String target, RoadSegment road, int weight) {}
    private record Previous(String node, RoadSegment road) {}
    private record NodeDistance(String node, int distance) {}
    private record PlaceMention(String name, int index) {}

    public record RouteRequest(String origin, String destination, Boolean accessibleOnly, Boolean avoidRestricted, String optimization) {
        public RouteRequest(String origin, String destination, Boolean accessibleOnly, Boolean avoidRestricted) {
            this(origin, destination, accessibleOnly, avoidRestricted, "DISTANCE");
        }
    }
    public record AiRouteRequest(String prompt, String origin) {}
    public record RouteStep(Long roadId, String roadName, String fromNode, String toNode,
                            int lengthMeters, String status, boolean accessible, String restrictionReason) {}
    public record RouteResponse(String origin, String destination, int distanceMeters, int estimatedMinutes,
                                boolean accessibleOnly, boolean avoidRestricted, String strategy, double confidence,
                                List<RouteStep> steps, List<String> evidence, List<String> blockedRoads, String optimization) {}
}
