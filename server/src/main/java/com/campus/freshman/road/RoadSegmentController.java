package com.campus.freshman.road;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/roads")
public class RoadSegmentController {
    private final RoadSegmentRepository repository;
    public RoadSegmentController(RoadSegmentRepository repository) { this.repository = repository; }

    @GetMapping public List<RoadSegment> list() { return repository.findAllByOrderByUpdatedAtDesc(); }
    @GetMapping("/{id}") public RoadSegment detail(@PathVariable Long id) { return find(id); }
    @PostMapping public RoadSegment create(@Valid @RequestBody RoadSegment road) { road.setId(null); return repository.save(road); }
    @PutMapping("/{id}") public RoadSegment update(@PathVariable Long id, @Valid @RequestBody RoadSegment input) {
        RoadSegment road = find(id);
        road.setName(input.getName()); road.setFromNode(input.getFromNode()); road.setToNode(input.getToNode());
        road.setLengthMeters(input.getLengthMeters()); road.setStatus(input.getStatus());
        road.setRestrictionReason(input.getRestrictionReason()); road.setAccessible(input.getAccessible());
        return repository.save(road);
    }
    @DeleteMapping("/{id}") public void delete(@PathVariable Long id) { repository.delete(find(id)); }
    private RoadSegment find(Long id) { return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("道路不存在")); }
}
