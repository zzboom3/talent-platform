package com.talent.platform.controller;

import com.talent.platform.common.Result;
import com.talent.platform.entity.Company;
import com.talent.platform.entity.Job;
import com.talent.platform.entity.User;
import com.talent.platform.repository.CompanyRepository;
import com.talent.platform.repository.JobRepository;
import com.talent.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobRepository jobRepo;
    private final CompanyRepository companyRepo;
    private final UserRepository userRepo;

    @GetMapping
    public Result<List<Job>> list() {
        return Result.ok(jobRepo.findAll());
    }

    @GetMapping("/{id}")
    public Result<Job> getById(@PathVariable Long id) {
        return jobRepo.findById(id).map(Result::ok).orElse(Result.fail("岗位不存在"));
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
        job.setCompany(company);
        return Result.ok(jobRepo.save(job));
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
        job.setTitle(updated.getTitle());
        job.setDescription(updated.getDescription());
        job.setRequirements(updated.getRequirements());
        job.setSalaryRange(updated.getSalaryRange());
        job.setCity(updated.getCity());
        job.setStatus(updated.getStatus());
        return Result.ok(jobRepo.save(job));
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
        jobRepo.deleteById(id);
        return Result.ok();
    }
}
