package com.talent.platform.controller;

import com.talent.platform.common.Result;
import com.talent.platform.entity.Job;
import com.talent.platform.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
public class MatchController {

    private final JobRepository jobRepo;

    @GetMapping
    public Result<List<Job>> match(@RequestParam(required = false, defaultValue = "") String skills) {
        if (skills.isBlank()) {
            return Result.ok(jobRepo.findAll());
        }
        return Result.ok(jobRepo.findByKeyword(skills));
    }
}
