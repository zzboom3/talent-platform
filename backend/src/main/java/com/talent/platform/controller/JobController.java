package com.talent.platform.controller;

import com.talent.platform.common.Result;
import com.talent.platform.entity.Company;
import com.talent.platform.entity.Job;
import com.talent.platform.entity.JobApplication;
import com.talent.platform.entity.User;
import com.talent.platform.repository.CompanyRepository;
import com.talent.platform.repository.JobApplicationRepository;
import com.talent.platform.repository.JobRepository;
import com.talent.platform.repository.UserRepository;
import com.talent.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobRepository jobRepo;
    private final CompanyRepository companyRepo;
    private final UserRepository userRepo;
    private final JobApplicationRepository appRepo;
    private final NotificationService notificationService;

    @GetMapping
    public Result<List<Job>> list() {
        List<Job> jobs = jobRepo.findAll().stream()
                .filter(j -> j.getCompany() != null
                        && j.getCompany().getAuditStatus() == Company.AuditStatus.APPROVED
                        && Boolean.TRUE.equals(j.getCompany().getVisible()))
                .collect(Collectors.toList());
        return Result.ok(jobs);
    }

    @GetMapping("/{id}")
    public Result<Job> getById(@PathVariable Long id) {
        return jobRepo.findById(id)
                .filter(job -> job.getCompany() != null
                        && job.getCompany().getAuditStatus() == Company.AuditStatus.APPROVED
                        && Boolean.TRUE.equals(job.getCompany().getVisible()))
                .map(Result::ok)
                .orElse(Result.fail("岗位不存在"));
    }

    @GetMapping("/my")
    public Result<List<Job>> myJobs(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        Company company = companyRepo.findByUser(user).orElse(null);
        if (company == null) return Result.ok(List.of());
        return Result.ok(jobRepo.findByCompany(company));
    }

    @PostMapping
    public Result<Job> create(@RequestBody Job job,
                              @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        Company company = companyRepo.findByUser(user).orElse(null);
        if (company == null) return Result.fail("请先创建企业信息");
        if (company.getAuditStatus() != Company.AuditStatus.APPROVED) {
            return Result.fail("企业尚未通过审核，无法发布岗位");
        }
        job.setCompany(company);
        Job saved = jobRepo.save(job);
        notificationService.notifyJobPublished(saved);
        return Result.ok(saved);
    }

    @PutMapping("/{id}")
    public Result<Job> update(@PathVariable Long id, @RequestBody Job updated,
                              @AuthenticationPrincipal UserDetails userDetails) {
        Job job = jobRepo.findById(id).orElse(null);
        if (job == null) return Result.fail("岗位不存在");
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        boolean isOwner = job.getCompany().getUser().getId().equals(user.getId());
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isOwner && !isAdmin) return Result.fail("无权限");
        String oldStatus = job.getStatus();
        job.setTitle(updated.getTitle());
        job.setDescription(updated.getDescription());
        job.setRequirements(updated.getRequirements());
        job.setSalaryRange(updated.getSalaryRange());
        job.setCity(updated.getCity());
        job.setStatus(updated.getStatus());
        job = jobRepo.save(job);
        if ("CLOSED".equals(updated.getStatus()) && !"CLOSED".equals(oldStatus)) {
            notificationService.notifyJobClosed(job, appRepo.findByJob(job));
        }
        return Result.ok(job);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id,
                            @AuthenticationPrincipal UserDetails userDetails) {
        Job job = jobRepo.findById(id).orElse(null);
        if (job == null) return Result.fail("岗位不存在");
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        boolean isOwner = job.getCompany().getUser().getId().equals(user.getId());
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isOwner && !isAdmin) return Result.fail("无权限");
        var applicants = appRepo.findByJob(job);
        notificationService.notifyJobClosed(job, applicants);
        jobRepo.deleteById(id);
        return Result.ok();
    }
}
