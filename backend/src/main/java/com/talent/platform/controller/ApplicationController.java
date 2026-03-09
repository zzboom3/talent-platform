package com.talent.platform.controller;

import com.talent.platform.common.Result;
import com.talent.platform.entity.Company;
import com.talent.platform.entity.Job;
import com.talent.platform.entity.JobApplication;
import com.talent.platform.entity.TalentProfile;
import com.talent.platform.entity.User;
import com.talent.platform.repository.CompanyRepository;
import com.talent.platform.repository.JobApplicationRepository;
import com.talent.platform.repository.JobRepository;
import com.talent.platform.repository.TalentProfileRepository;
import com.talent.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final JobApplicationRepository appRepo;
    private final TalentProfileRepository talentRepo;
    private final JobRepository jobRepo;
    private final UserRepository userRepo;
    private final CompanyRepository companyRepo;

    @PostMapping
    public Result<?> apply(@RequestBody Map<String, Long> body,
                           @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        TalentProfile talent = talentRepo.findByUser(user).orElse(null);
        if (talent == null) return Result.fail("请先创建人才档案");
        Long jobId = body.get("jobId");
        Job job = jobRepo.findById(jobId).orElse(null);
        if (job == null) return Result.fail("岗位不存在");
        if (appRepo.existsByTalentAndJobId(talent, jobId)) return Result.fail("已申请过该岗位");
        JobApplication app = new JobApplication();
        app.setTalent(talent);
        app.setJob(job);
        return Result.ok(appRepo.save(app));
    }

    @GetMapping("/my")
    public Result<List<JobApplication>> myApplications(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        TalentProfile talent = talentRepo.findByUser(user).orElse(null);
        if (talent == null) return Result.ok(List.of());
        return Result.ok(appRepo.findByTalent(talent));
    }

    @GetMapping("/company")
    public Result<List<JobApplication>> companyApplications(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        Company company = companyRepo.findByUser(user).orElse(null);
        if (company == null) return Result.ok(List.of());
        return Result.ok(appRepo.findByJobCompanyId(company.getId()));
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id,
                                  @RequestBody Map<String, String> body) {
        JobApplication app = appRepo.findById(id).orElse(null);
        if (app == null) return Result.fail("申请不存在");
        try {
            app.setStatus(JobApplication.Status.valueOf(body.get("status").toUpperCase()));
        } catch (Exception e) {
            return Result.fail("状态值无效");
        }
        return Result.ok(appRepo.save(app));
    }
}
