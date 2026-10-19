package com.campus.freshman.navigation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NavigationPlanRepository extends JpaRepository<NavigationPlanLog, Long> {
    List<NavigationPlanLog> findTop20ByOrderByCreatedAtDesc();
}
