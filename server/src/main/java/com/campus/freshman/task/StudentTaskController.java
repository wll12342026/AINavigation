package com.campus.freshman.task;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class StudentTaskController {
    private final StudentTaskRepository repository;
    public StudentTaskController(StudentTaskRepository repository) { this.repository = repository; }

    @GetMapping public List<StudentTask> list() { return repository.findAllByDemoDataFalseOrderBySortOrderAsc(); }
    @GetMapping("/{id}") public StudentTask detail(@PathVariable Long id) { return find(id); }
    @PostMapping public StudentTask create(@Valid @RequestBody StudentTask task) { task.setId(null); return repository.save(task); }
    @PutMapping("/{id}") public StudentTask update(@PathVariable Long id, @Valid @RequestBody StudentTask input) {
        StudentTask task = find(id);
        task.setName(input.getName()); task.setDescription(input.getDescription()); task.setLocation(input.getLocation());
        task.setStatus(input.getStatus()); task.setProgress(input.getProgress()); task.setCompletedCount(input.getCompletedCount());
        task.setTotalCount(input.getTotalCount()); task.setEstimatedMinutes(input.getEstimatedMinutes()); task.setSortOrder(input.getSortOrder());
        return repository.save(task);
    }
    @DeleteMapping("/{id}") public void delete(@PathVariable Long id) { repository.delete(find(id)); }
    private StudentTask find(Long id) { return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("新生任务不存在")); }
}
