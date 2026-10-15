package com.campus.freshman.road;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoadSegmentRepository extends JpaRepository<RoadSegment, Long> {
    List<RoadSegment> findAllByOrderByUpdatedAtDesc();
    List<RoadSegment> findAllByDemoDataFalseOrderByUpdatedAtDesc();
    long countByStatus(String status);
    long countByStatusAndDemoDataFalse(String status);
}
