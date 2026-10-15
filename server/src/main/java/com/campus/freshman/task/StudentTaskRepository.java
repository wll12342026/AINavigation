package com.campus.freshman.task;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentTaskRepository extends JpaRepository<StudentTask, Long> {
    List<StudentTask> findAllByOrderBySortOrderAsc();
}
