package com.campus.freshman.settings;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SystemSettingController {
    private final SystemSettingRepository repository;

    public SystemSettingController(SystemSettingRepository repository) { this.repository = repository; }

    @GetMapping
    @Transactional
    public Map<String, String> getAll() {
        Map<String, String> defaults = Map.of(
                "modelName", "",
                "multimodal", "false",
                "tools", "true",
                "citations", "true",
                "refuseUnknown", "true",
                "confidence", "80",
                "memoryTurns", "10"
        );
        defaults.forEach((key, value) -> {
            if (repository.findByKey(key).isEmpty()) {
                SystemSetting item = new SystemSetting(); item.setKey(key); item.setValue(value); repository.save(item);
            }
        });
        Map<String, String> result = new LinkedHashMap<>();
        repository.findAll().forEach(item -> result.put(item.getKey(), item.getValue()));
        return result;
    }

    @PutMapping
    @Transactional
    public Map<String, String> save(@RequestBody Map<String, String> values) {
        values.forEach((key, value) -> {
            SystemSetting item = repository.findByKey(key).orElseGet(SystemSetting::new);
            item.setKey(key); item.setValue(value); repository.save(item);
        });
        return getAll();
    }
}
