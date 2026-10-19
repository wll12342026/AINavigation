package com.campus.freshman.navigation;

import com.campus.freshman.place.Place;
import com.campus.freshman.place.PlaceRepository;
import com.campus.freshman.road.RoadSegment;
import com.campus.freshman.road.RoadSegmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NavigationServiceTest {
    @Mock RoadSegmentRepository roads;
    @Mock PlaceRepository places;
    @Mock NavigationPlanRepository plans;
    private NavigationService service;

    @BeforeEach
    void setUp() {
        service = new NavigationService(roads, places, plans);
        lenient().when(plans.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void avoidsRestrictedRoadAndBuildsEvidence() {
        when(roads.findAllByDemoDataFalseOrderByUpdatedAtDesc()).thenReturn(List.of(
                road(1L, "迎新路", "东门", "报到点", 100, "OPEN", true),
                road(2L, "明德路", "报到点", "图书馆", 120, "OPEN", true),
                road(3L, "施工捷径", "东门", "图书馆", 50, "RESTRICTED", false)
        ));

        NavigationService.RouteResponse result = service.plan(
                new NavigationService.RouteRequest("东门", "图书馆", false, true));

        assertThat(result.distanceMeters()).isEqualTo(220);
        assertThat(result.steps()).extracting(NavigationService.RouteStep::roadName)
                .containsExactly("迎新路", "明德路");
        assertThat(result.blockedRoads()).anyMatch(value -> value.contains("施工捷径"));
    }

    @Test
    void interpretsAliasesAndAccessibilityFromPrompt() {
        when(roads.findAllByDemoDataFalseOrderByUpdatedAtDesc()).thenReturn(List.of(
                road(1L, "【测试】迎新路", "【测试】 东门", "【测试】 图书馆", 300, "OPEN", true)
        ));
        Place origin = place(11L, "【测试】 东门");
        Place destination = place(12L, "【测试】 图书馆");
        when(places.findAllByDemoDataFalseOrderByUpdatedAtDesc()).thenReturn(List.of(destination, origin));

        NavigationService.RouteResponse result = service.planFromPrompt(
                new NavigationService.AiRouteRequest("我要从东门坐轮椅去图书馆", null));

        assertThat(result.accessibleOnly()).isTrue();
        assertThat(result.origin()).isEqualTo("【测试】 东门");
        assertThat(result.destination()).isEqualTo("【测试】 图书馆");
        assertThat(result.strategy()).isEqualTo("SEMANTIC_ROUTE");
    }

    @Test
    void timeStrategyCanChooseLongerButFasterRoute() {
        RoadSegment shortRoad = road(1L, "慢行近路", "东门", "图书馆", 100, "OPEN", true);
        shortRoad.setTravelSeconds(600);
        RoadSegment fastRoad = road(2L, "通畅远路", "东门", "图书馆", 300, "OPEN", true);
        fastRoad.setTravelSeconds(120);
        when(roads.findAllByDemoDataFalseOrderByUpdatedAtDesc()).thenReturn(List.of(shortRoad, fastRoad));
        assertThat(service.plan(new NavigationService.RouteRequest("东门", "图书馆", false, true, "DISTANCE")).distanceMeters()).isEqualTo(100);
        var fastest = service.plan(new NavigationService.RouteRequest("东门", "图书馆", false, true, "TIME"));
        assertThat(fastest.distanceMeters()).isEqualTo(300);
        assertThat(fastest.estimatedMinutes()).isEqualTo(2);
        assertThat(fastest.optimization()).isEqualTo("TIME");
    }

    @Test
    void oneWayRoadCannotBeTraversedBackwards() {
        RoadSegment oneWay = road(1L, "单行路", "东门", "图书馆", 100, "OPEN", true);
        oneWay.setBidirectional(false);
        when(roads.findAllByDemoDataFalseOrderByUpdatedAtDesc()).thenReturn(List.of(oneWay));
        assertThat(service.plan(new NavigationService.RouteRequest("东门", "图书馆", false, true)).distanceMeters()).isEqualTo(100);
        assertThatThrownBy(() -> service.plan(new NavigationService.RouteRequest("图书馆", "东门", false, true)))
                .hasMessageContaining("不存在可达路线");
    }

    @Test
    void freshPlanReroutesAfterRoadClosure() {
        RoadSegment shortcut = road(1L, "近路", "东门", "图书馆", 100, "OPEN", true);
        when(roads.findAllByDemoDataFalseOrderByUpdatedAtDesc()).thenReturn(List.of(shortcut,
                road(2L, "第一段", "东门", "报到处", 100, "OPEN", true),
                road(3L, "第二段", "报到处", "图书馆", 100, "OPEN", true)));
        var request = new NavigationService.RouteRequest("东门", "图书馆", false, true);
        assertThat(service.plan(request).distanceMeters()).isEqualTo(100);
        shortcut.setStatus("CLOSED");
        assertThat(service.plan(request).distanceMeters()).isEqualTo(200);
    }

    @Test
    void resolvesStoredPlaceAliases() {
        when(roads.findAllByDemoDataFalseOrderByUpdatedAtDesc()).thenReturn(List.of(
                road(1L, "服务路", "东门", "校园卡中心", 100, "OPEN", true)));
        Place center = place(2L, "校园卡中心"); center.setAliases("一卡通中心，领卡处");
        when(places.findAllByDemoDataFalseOrderByUpdatedAtDesc()).thenReturn(List.of(place(1L, "东门"), center));
        var result = service.planFromPrompt(new NavigationService.AiRouteRequest("从东门去领卡处", null));
        assertThat(result.destination()).isEqualTo("校园卡中心");
    }

    @Test
    void refusesClosedDestination() {
        when(roads.findAllByDemoDataFalseOrderByUpdatedAtDesc()).thenReturn(List.of(
                road(1L, "服务路", "东门", "图书馆", 100, "OPEN", true)));
        Place library = place(2L, "图书馆"); library.setStatus("CLOSED");
        when(places.findAllByDemoDataFalseOrderByUpdatedAtDesc()).thenReturn(List.of(library));
        assertThatThrownBy(() -> service.plan(new NavigationService.RouteRequest("东门", "图书馆", false, true)))
                .hasMessageContaining("当前未开放");
    }

    @Test
    void doesNotSilentlyDropIntermediateDestinations() {
        when(places.findAllByDemoDataFalseOrderByUpdatedAtDesc()).thenReturn(List.of(
                place(1L, "东门"), place(2L, "报到处"), place(3L, "图书馆")));
        assertThatThrownBy(() -> service.planFromPrompt(new NavigationService.AiRouteRequest("从东门先去报到处再去图书馆", null)))
                .hasMessageContaining("多事项路线尚未启用");
    }

    @Test
    void refusesInaccessibleOnlyRoute() {
        when(roads.findAllByDemoDataFalseOrderByUpdatedAtDesc()).thenReturn(List.of(
                road(1L, "楼梯", "东门", "图书馆", 100, "OPEN", false)));
        assertThatThrownBy(() -> service.plan(new NavigationService.RouteRequest("东门", "图书馆", true, true)))
                .hasMessageContaining("尚未接入当前可用路网");
    }

    @Test
    void returnsZeroForSameConnectedNode() {
        when(roads.findAllByDemoDataFalseOrderByUpdatedAtDesc()).thenReturn(List.of(
                road(1L, "服务路", "东门", "图书馆", 100, "OPEN", true)));
        var result = service.plan(new NavigationService.RouteRequest("东门", "东门", false, true));
        assertThat(result.distanceMeters()).isZero();
        assertThat(result.estimatedMinutes()).isZero();
        assertThat(result.steps()).isEmpty();
    }

    private RoadSegment road(Long id, String name, String from, String to, int length, String status, boolean accessible) {
        RoadSegment road = new RoadSegment();
        road.setId(id); road.setName(name); road.setFromNode(from); road.setToNode(to);
        road.setLengthMeters(length); road.setStatus(status); road.setAccessible(accessible);
        return road;
    }

    private Place place(Long id, String name) {
        Place place = new Place();
        place.setId(id); place.setName(name); place.setCategory("BUILDING");
        return place;
    }
}
