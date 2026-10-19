package com.campus.freshman.media;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {
    List<MediaAsset> findAllByDemoDataFalseOrderByUpdatedAtDesc();
    Optional<MediaAsset> findFirstByFileHashAndDemoDataFalse(String fileHash);
}
