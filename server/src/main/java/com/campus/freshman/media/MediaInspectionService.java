package com.campus.freshman.media;

import com.campus.freshman.place.Place;
import com.campus.freshman.place.PlaceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class MediaInspectionService {
    private final MediaAssetRepository media;
    private final PlaceRepository places;
    private final Path uploadDirectory;

    public MediaInspectionService(MediaAssetRepository media, PlaceRepository places,
                                  @Value("${campus.upload-dir:data/uploads}") String uploadDirectory) {
        this.media = media;
        this.places = places;
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public MediaAsset upload(MultipartFile file, String title, Long placeId, String type) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择需要上传的图片");
        }
        if (file.getSize() > 15L * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "图片不能超过 15MB");
        }

        try {
            byte[] bytes = file.getBytes();
            BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            if (image == null) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "仅支持可识别的 JPG、PNG 或 WebP 图片");
            }

            String hash = sha256(bytes);
            MediaAsset duplicate = media.findFirstByFileHashAndDemoDataFalse(hash).orElse(null);
            Analysis analysis = analyze(image);
            Place place = placeId == null ? null : places.findById(placeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "绑定地点不存在"));
            String extension = extension(file.getOriginalFilename());
            String storedName = UUID.randomUUID().toString().replace("-", "") + extension;
            Files.createDirectories(uploadDirectory);
            Path target = uploadDirectory.resolve(storedName).normalize();
            if (!target.startsWith(uploadDirectory)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件名不合法");
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }

            boolean isDuplicate = duplicate != null;
            String issue = isDuplicate ? "DUPLICATE" : analysis.score() < 70 ? "LOW_QUALITY" : null;

            MediaAsset asset = new MediaAsset();
            asset.setTitle(title == null || title.isBlank() ? safeTitle(file.getOriginalFilename()) : title.trim());
            asset.setPlaceId(place == null ? null : place.getId());
            asset.setPlaceName(place == null ? "" : place.getName());
            asset.setType(type == null || type.isBlank() ? "PHOTO" : type);
            asset.setUrl("/api/media/files/" + storedName);
            asset.setFileName(storedName);
            asset.setFileSize(file.getSize());
            asset.setImageWidth(image.getWidth());
            asset.setImageHeight(image.getHeight());
            asset.setFileHash(hash);
            asset.setVisualFingerprint(analysis.fingerprint());
            asset.setBrightnessScore(analysis.brightness());
            asset.setSharpnessScore(analysis.sharpness());
            asset.setQualityScore(isDuplicate ? Math.min(analysis.score(), 60) : analysis.score());
            asset.setIssueType(issue);
            asset.setReviewStatus(issue == null && analysis.score() >= 80 ? "PASSED" : "PENDING");
            asset.setInspectionEvidence(buildEvidence(image, analysis, duplicate));
            return media.save(asset);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "图片保存失败", exception);
        }
    }

    public RecognitionResult recognize(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择校园图片");
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "无法读取图片内容");
            String fingerprint = fingerprint(image);
            List<RecognitionCandidate> candidates = media.findAllByDemoDataFalseOrderByUpdatedAtDesc().stream()
                    .filter(item -> item.getPlaceId() != null && item.getVisualFingerprint() != null)
                    .map(item -> {
                        int distance = hamming(fingerprint, item.getVisualFingerprint());
                        double confidence = Math.max(0, Math.min(0.98, 1.0 - distance / 64.0));
                        return new RecognitionCandidate(item.getPlaceId(), item.getPlaceName(), item.getUrl(),
                                Math.round(confidence * 1000.0) / 1000.0,
                                "视觉指纹相差 " + distance + " / 64 位");
                    })
                    .sorted(Comparator.comparingDouble(RecognitionCandidate::confidence).reversed())
                    .limit(3)
                    .toList();
            boolean sufficient = !candidates.isEmpty() && candidates.getFirst().confidence() >= 0.62;
            return new RecognitionResult(sufficient,
                    sufficient ? "已匹配到相似校园地标" : "现有地点图库证据不足，请补充更多角度的已绑定照片",
                    "LOCAL_VISUAL_FINGERPRINT", candidates);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片读取失败", exception);
        }
    }

    public Path resolveFile(String fileName) {
        Path target = uploadDirectory.resolve(fileName).normalize();
        if (!target.startsWith(uploadDirectory) || !Files.exists(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "素材文件不存在");
        }
        return target;
    }

    public void deleteStoredFile(MediaAsset asset) {
        if (asset == null || asset.getFileName() == null || asset.getFileName().isBlank()) return;
        Path target = uploadDirectory.resolve(asset.getFileName()).normalize();
        if (!target.startsWith(uploadDirectory)) return;
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "素材文件删除失败", exception);
        }
    }

    private Analysis analyze(BufferedImage image) {
        long brightnessTotal = 0;
        long contrastTotal = 0;
        long samples = 0;
        int step = Math.max(1, Math.min(image.getWidth(), image.getHeight()) / 220);
        for (int y = 0; y < image.getHeight(); y += step) {
            int previous = -1;
            for (int x = 0; x < image.getWidth(); x += step) {
                int gray = gray(image.getRGB(x, y));
                brightnessTotal += gray;
                if (previous >= 0) contrastTotal += Math.abs(gray - previous);
                previous = gray;
                samples++;
            }
        }
        double average = samples == 0 ? 0 : brightnessTotal / (double) samples;
        double contrast = samples == 0 ? 0 : contrastTotal / (double) samples;
        int brightness = (int) Math.round(Math.max(0, 100 - Math.abs(average - 132) * 1.2));
        int sharpness = (int) Math.round(Math.max(0, Math.min(100, contrast * 5.5)));
        int resolution = Math.min(100, (int) Math.round(Math.sqrt((long) image.getWidth() * image.getHeight()) / 14.0));
        int score = (int) Math.round(resolution * 0.35 + brightness * 0.25 + sharpness * 0.40);
        return new Analysis(Math.max(0, Math.min(100, score)), brightness, sharpness, fingerprint(image));
    }

    private String buildEvidence(BufferedImage image, Analysis analysis, MediaAsset duplicate) {
        List<String> items = new ArrayList<>();
        items.add("分辨率 " + image.getWidth() + "×" + image.getHeight());
        items.add("曝光得分 " + analysis.brightness());
        items.add("边缘清晰度得分 " + analysis.sharpness());
        if (duplicate != null) items.add("文件与“" + duplicate.getTitle() + "”完全重复");
        else if (analysis.score() < 70) items.add("建议重新拍摄更清晰、曝光更均衡的照片");
        else items.add("未发现完全重复文件");
        return String.join("；", items);
    }

    private String fingerprint(BufferedImage image) {
        int[] values = new int[64];
        long total = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int sourceX = Math.min(image.getWidth() - 1, (int) ((x + 0.5) * image.getWidth() / 8.0));
                int sourceY = Math.min(image.getHeight() - 1, (int) ((y + 0.5) * image.getHeight() / 8.0));
                int value = gray(image.getRGB(sourceX, sourceY));
                values[y * 8 + x] = value;
                total += value;
            }
        }
        double average = total / 64.0;
        long bits = 0;
        for (int i = 0; i < values.length; i++) if (values[i] >= average) bits |= (1L << i);
        return String.format("%016x", bits);
    }

    private int hamming(String left, String right) {
        try {
            return Long.bitCount(Long.parseUnsignedLong(left, 16) ^ Long.parseUnsignedLong(right, 16));
        } catch (NumberFormatException exception) {
            return 64;
        }
    }

    private int gray(int rgb) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        return (red * 30 + green * 59 + blue * 11) / 100;
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private String extension(String originalName) {
        if (originalName == null) return ".jpg";
        int dot = originalName.lastIndexOf('.');
        if (dot < 0) return ".jpg";
        String value = originalName.substring(dot).toLowerCase(Locale.ROOT);
        return List.of(".jpg", ".jpeg", ".png", ".webp").contains(value) ? value : ".jpg";
    }

    private String safeTitle(String originalName) {
        if (originalName == null || originalName.isBlank()) return "未命名校园实景";
        int dot = originalName.lastIndexOf('.');
        return dot > 0 ? originalName.substring(0, dot) : originalName;
    }

    private record Analysis(int score, int brightness, int sharpness, String fingerprint) {}
    public record RecognitionCandidate(Long placeId, String placeName, String referenceUrl,
                                       double confidence, String evidence) {}
    public record RecognitionResult(boolean sufficientEvidence, String message, String method,
                                    List<RecognitionCandidate> candidates) {}
}
