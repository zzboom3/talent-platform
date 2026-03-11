package com.talent.platform.controller;

import com.talent.platform.common.Result;
import com.talent.platform.entity.Company;
import com.talent.platform.entity.Job;
import com.talent.platform.entity.TalentProfile;
import com.talent.platform.repository.JobRepository;
import com.talent.platform.repository.TalentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
public class MatchController {

    private final JobRepository jobRepo;
    private final TalentProfileRepository talentRepo;

    @GetMapping
    public Result<List<Job>> match(@RequestParam(required = false, defaultValue = "") String skills) {
        List<Job> jobs;
        if (skills.isBlank()) {
            jobs = jobRepo.findAll();
        } else {
            jobs = jobRepo.findByKeyword(skills);
        }
        List<Job> filtered = jobs.stream()
                .filter(j -> j.getCompany() != null
                        && j.getCompany().getAuditStatus() == Company.AuditStatus.APPROVED
                        && Boolean.TRUE.equals(j.getCompany().getVisible()))
                .collect(Collectors.toList());
        return Result.ok(filtered);
    }

    @GetMapping("/talents")
    public Result<List<TalentProfile>> matchTalents(@RequestParam(required = false, defaultValue = "") String keyword) {
        List<TalentProfile> talents;
        if (keyword.isBlank()) {
            talents = talentRepo.findByVisibleTrue();
        } else {
            talents = talentRepo.findByKeyword(keyword).stream()
                    .filter(t -> Boolean.TRUE.equals(t.getVisible()))
                    .collect(Collectors.toList());
        }
        return Result.ok(talents);
    }
}
