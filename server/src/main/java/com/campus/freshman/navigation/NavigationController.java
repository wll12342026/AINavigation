package com.campus.freshman.navigation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/navigation")
public class NavigationController {
    private final NavigationService service;

    public NavigationController(NavigationService service) {
        this.service = service;
    }

    @PostMapping("/plan")
    public NavigationService.RouteResponse plan(@RequestBody NavigationService.RouteRequest request) {
        return service.plan(request);
    }

    @PostMapping("/ai-plan")
    public NavigationService.RouteResponse aiPlan(@RequestBody NavigationService.AiRouteRequest request) {
        return service.planFromPrompt(request);
    }

    @GetMapping("/history")
    public List<NavigationPlanLog> history() {
        return service.history();
    }
}
