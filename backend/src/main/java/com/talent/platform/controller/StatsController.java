package com.talent.platform.controller;

import com.talent.platform.common.Result;
import com.talent.platform.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping
    public Result<Map<String, Object>> publicStats() {
        return Result.ok(statsService.getPublicStats());
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboardStats() {
        return Result.ok(statsService.getDashboardStats());
    }

    @GetMapping("/monthly-trend")
    public Result<Map<String, Object>> monthlyTrend() {
        return Result.ok(statsService.getMonthlyTrend());
    }

    @GetMapping("/application-status")
    public Result<Map<String, Long>> applicationStatusStats() {
        return Result.ok(statsService.getApplicationStatusStats());
    }
}
