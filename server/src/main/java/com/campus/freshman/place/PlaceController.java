package com.campus.freshman.place;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/places")
public class PlaceController {
    private final PlaceRepository repository;

    public PlaceController(PlaceRepository repository) { this.repository = repository; }

    @GetMapping
    public List<Place> list() { return repository.findAllByDemoDataFalseOrderByUpdatedAtDesc(); }

    @GetMapping("/{id}")
    public Place detail(@PathVariable Long id) { return find(id); }

    @PostMapping
    public Place create(@Valid @RequestBody Place place) { place.setId(null); return repository.save(place); }

    @PutMapping("/{id}")
    public Place update(@PathVariable Long id, @Valid @RequestBody Place input) {
        Place place = find(id);
        place.setName(input.getName());
        place.setCategory(input.getCategory());
        place.setBuilding(input.getBuilding());
        place.setFloor(input.getFloor());
        place.setStatus(input.getStatus());
        place.setLongitude(input.getLongitude());
        place.setLatitude(input.getLatitude());
        place.setEntranceHint(input.getEntranceHint());
        place.setDescription(input.getDescription());
        return repository.save(place);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { repository.delete(find(id)); }

    private Place find(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("校园地点不存在"));
    }
}
