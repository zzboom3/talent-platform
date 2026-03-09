package com.talent.platform.controller;

import com.talent.platform.common.Result;
import com.talent.platform.entity.User;
import com.talent.platform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepo;
    private final TalentProfileRepository talentRepo;
    private final JobRepository jobRepo;
    private final NewsRepository newsRepo;
    private final CourseRepository courseRepo;
    private final JobApplicationRepository appRepo;

    @GetMapping("/users")
    public Result<List<User>> listUsers() {
        return Result.ok(userRepo.findAll());
    }

    @DeleteMapping("/users/{id}")
    public Result<?> deleteUser(@PathVariable Long id) {
        userRepo.deleteById(id);
        return Result.ok();
    }

    @PutMapping("/users/{id}/role")
    public Result<?> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = userRepo.findById(id).orElse(null);
        if (user == null) return Result.fail("用户不存在");
        try {
            user.setRole(User.Role.valueOf(body.get("role").toUpperCase()));
            userRepo.save(user);
        } catch (Exception e) {
            return Result.fail("角色值无效");
        }
        return Result.ok();
    }

    @GetMapping("/stats")
    public Result<Map<String, Long>> stats() {
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
