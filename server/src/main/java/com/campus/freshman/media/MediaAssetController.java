package com.campus.freshman.media;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/media")
public class MediaAssetController {
    private final MediaAssetRepository repository;

    public MediaAssetController(MediaAssetRepository repository) { this.repository = repository; }

    @GetMapping
    public List<MediaAsset> list() { return repository.findAllByOrderByUpdatedAtDesc(); }

    @GetMapping("/summary")
    public MediaSummary summary() {
        List<MediaAsset> items = repository.findAll();
        long scored = items.stream().filter(item -> item.getQualityScore() != null).count();
        double passRate = scored == 0 ? 0 : items.stream()
                .filter(item -> item.getQualityScore() != null && item.getQualityScore() >= 80).count() * 100.0 / scored;
        long boundPlaces = items.stream().map(MediaAsset::getPlaceId).filter(id -> id != null).distinct().count();
        return new MediaSummary(items.size(), boundPlaces,
                Math.round(passRate * 10.0) / 10.0, repository.countByReviewStatus("PENDING"),
                repository.countByIssueType("LOW_QUALITY"), repository.countByIssueType("WRONG_DIRECTION"),
                repository.countByIssueType("DUPLICATE"));
    }

    @PostMapping
    public MediaAsset create(@Valid @RequestBody MediaAsset item) { item.setId(null); return repository.save(item); }

    @PutMapping("/{id}")
    public MediaAsset update(@PathVariable Long id, @Valid @RequestBody MediaAsset input) {
        MediaAsset item = find(id);
        item.setTitle(input.getTitle()); item.setPlaceId(input.getPlaceId()); item.setPlaceName(input.getPlaceName());
        item.setType(input.getType()); item.setUrl(input.getUrl()); item.setQualityScore(input.getQualityScore());
        item.setReviewStatus(input.getReviewStatus()); item.setIssueType(input.getIssueType());
        return repository.save(item);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { repository.delete(find(id)); }

    private MediaAsset find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "素材不存在"));
    }

    public record MediaSummary(long total, long boundPlaces, double passRate, long pending,
                               long lowQuality, long wrongDirection, long duplicate) {}
}
