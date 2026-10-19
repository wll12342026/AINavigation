package com.campus.freshman.media;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/media")
public class MediaAssetController {
    private final MediaAssetRepository repository;
    private final MediaInspectionService inspectionService;

    public MediaAssetController(MediaAssetRepository repository, MediaInspectionService inspectionService) {
        this.repository = repository;
        this.inspectionService = inspectionService;
    }

    @GetMapping
    public List<MediaAsset> list() { return repository.findAllByDemoDataFalseOrderByUpdatedAtDesc(); }

    @GetMapping("/summary")
    public MediaSummary summary() {
        List<MediaAsset> items = repository.findAllByDemoDataFalseOrderByUpdatedAtDesc();
        long scored = items.stream().filter(item -> item.getQualityScore() != null).count();
        double passRate = scored == 0 ? 0 : items.stream()
                .filter(item -> item.getQualityScore() != null && item.getQualityScore() >= 80).count() * 100.0 / scored;
        long boundPlaces = items.stream().map(MediaAsset::getPlaceId).filter(id -> id != null).distinct().count();
        return new MediaSummary(items.size(), boundPlaces,
                Math.round(passRate * 10.0) / 10.0,
                items.stream().filter(item -> "PENDING".equals(item.getReviewStatus())).count(),
                items.stream().filter(item -> "LOW_QUALITY".equals(item.getIssueType())).count(),
                items.stream().filter(item -> "WRONG_DIRECTION".equals(item.getIssueType())).count(),
                items.stream().filter(item -> "DUPLICATE".equals(item.getIssueType())).count());
    }

    @PostMapping
    public MediaAsset create(@Valid @RequestBody MediaAsset item) { item.setId(null); return repository.save(item); }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MediaAsset upload(@RequestPart("file") MultipartFile file,
                             @RequestParam(required = false) String title,
                             @RequestParam(required = false) Long placeId,
                             @RequestParam(defaultValue = "PHOTO") String type) {
        return inspectionService.upload(file, title, placeId, type);
    }

    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MediaInspectionService.RecognitionResult recognize(@RequestPart("file") MultipartFile file) {
        return inspectionService.recognize(file);
    }

    @GetMapping("/files/{fileName:.+}")
    public ResponseEntity<Resource> file(@PathVariable String fileName) {
        Path path = inspectionService.resolveFile(fileName);
        try {
            String contentType = Files.probeContentType(path);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType == null ? "application/octet-stream" : contentType))
                    .body(new FileSystemResource(path));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "素材文件读取失败", exception);
        }
    }

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
    public void delete(@PathVariable Long id) {
        MediaAsset item = find(id);
        inspectionService.deleteStoredFile(item);
        repository.delete(item);
    }

    private MediaAsset find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "素材不存在"));
    }

    public record MediaSummary(long total, long boundPlaces, double passRate, long pending,
                               long lowQuality, long wrongDirection, long duplicate) {}
}
