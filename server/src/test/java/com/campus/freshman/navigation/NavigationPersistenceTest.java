package com.campus.freshman.navigation;

import com.campus.freshman.place.*;
import com.campus.freshman.road.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class NavigationPersistenceTest {
    @Autowired PlaceRepository places;
    @Autowired RoadSegmentRepository roads;
    @Autowired NavigationService navigation;
    @Autowired NavigationPlanRepository plans;
    @Autowired EntityManager entities;

    @Test
    void fieldsSurviveDatabaseReloadAndPlanHistoryRecordsStrategy() {
        Place place = new Place(); place.setName("持久化测试图书馆"); place.setCategory("LIBRARY");
        place.setAliases("测试阅览中心"); place.setOpeningHours("08:00—22:00");
        Long placeId = places.saveAndFlush(place).getId();
        RoadSegment road = new RoadSegment(); road.setName("持久化测试路"); road.setFromNode("持久化测试东门");
        road.setToNode(place.getName()); road.setLengthMeters(180); road.setTravelSeconds(240); road.setBidirectional(false);
        Long roadId = roads.saveAndFlush(road).getId(); entities.clear();
        assertThat(places.findById(placeId).orElseThrow().getOpeningHours()).isEqualTo("08:00—22:00");
        assertThat(roads.findById(roadId).orElseThrow().getBidirectional()).isFalse();
        var result = navigation.plan(new NavigationService.RouteRequest("持久化测试东门", "测试阅览中心", false, true, "TIME"));
        assertThat(result.estimatedMinutes()).isEqualTo(4);
        assertThat(plans.findTop20ByOrderByCreatedAtDesc()).anyMatch(log -> "TIME".equals(log.getOptimization()));
    }
}
