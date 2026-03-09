package com.talent.platform.controller;

import com.talent.platform.common.Result;
import com.talent.platform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 公开统计接口（/api/stats），无需登录，供首页展示使用
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final UserRepository userRepo;
    private final TalentProfileRepository talentRepo;
    private final JobRepository jobRepo;
    private final NewsRepository newsRepo;
    private final CourseRepository courseRepo;
    private final JobApplicationRepository appRepo;

    @GetMapping
    public Result<Map<String, Long>> publicStats() {
        return Result.ok(Map.of(
                "users", userRepo.count(),
                "talents", talentRepo.count(),
                "jobs", jobRepo.count(),
                "news", newsRepo.count(),
                "courses", courseRepo.count(),
                "applications", appRepo.count()
        ));
    }
}
