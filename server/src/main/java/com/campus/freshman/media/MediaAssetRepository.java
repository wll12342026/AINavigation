package com.campus.freshman.media;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {
    List<MediaAsset> findAllByOrderByUpdatedAtDesc();
    long countByReviewStatus(String reviewStatus);
    long countByIssueType(String issueType);
    long countByPlaceIdIsNotNull();
}
