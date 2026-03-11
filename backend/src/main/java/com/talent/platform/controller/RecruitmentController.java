package com.talent.platform.controller;

import com.talent.platform.common.Result;
import com.talent.platform.entity.Company;
import com.talent.platform.entity.TalentRecruitment;
import com.talent.platform.entity.User;
import com.talent.platform.repository.CompanyRepository;
import com.talent.platform.repository.TalentRecruitmentRepository;
import com.talent.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recruitment")
@RequiredArgsConstructor
public class RecruitmentController {

    private final TalentRecruitmentRepository recruitRepo;
    private final CompanyRepository companyRepo;
    private final UserRepository userRepo;

    @GetMapping
    public Result<List<TalentRecruitment>> list() {
        return Result.ok(recruitRepo.findAllByOrderByCreateTimeDesc());
    }

    @GetMapping("/open")
    public Result<List<TalentRecruitment>> listOpen() {
        return Result.ok(recruitRepo.findByStatusOrderByCreateTimeDesc(TalentRecruitment.Status.OPEN));
    }

    @GetMapping("/{id}")
    public Result<TalentRecruitment> getById(@PathVariable Long id) {
        return recruitRepo.findById(id)
                .map(Result::ok)
                .orElse(Result.fail("引进需求不存在"));
    }

    @GetMapping("/my")
    public Result<List<TalentRecruitment>> myRecruitments(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        Company company = companyRepo.findByUser(user).orElse(null);
        if (company == null) return Result.ok(List.of());
        return Result.ok(recruitRepo.findByCompanyId(company.getId()));
    }

    @PostMapping
    public Result<TalentRecruitment> create(@RequestBody TalentRecruitment recruitment,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        Company company = companyRepo.findByUser(user).orElse(null);
        if (company == null) return Result.fail("请先创建企业信息");
        if (company.getAuditStatus() != Company.AuditStatus.APPROVED) {
            return Result.fail("企业尚未通过审核");
        }
        recruitment.setCompany(company);
        return Result.ok(recruitRepo.save(recruitment));
    }

    @PutMapping("/{id}")
    public Result<TalentRecruitment> update(@PathVariable Long id,
                                            @RequestBody TalentRecruitment updated,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        TalentRecruitment recruitment = recruitRepo.findById(id).orElse(null);
        if (recruitment == null) return Result.fail("引进需求不存在");
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        boolean isOwner = recruitment.getCompany() != null
                && recruitment.getCompany().getUser().getId().equals(user.getId());
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isOwner && !isAdmin) return Result.fail("无权限");
        recruitment.setTitle(updated.getTitle());
        recruitment.setRequirements(updated.getRequirements());
        recruitment.setTargetSkills(updated.getTargetSkills());
        recruitment.setCity(updated.getCity());
        recruitment.setSalaryRange(updated.getSalaryRange());
        if (updated.getStatus() != null) recruitment.setStatus(updated.getStatus());
        return Result.ok(recruitRepo.save(recruitment));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id,
                            @AuthenticationPrincipal UserDetails userDetails) {
        TalentRecruitment recruitment = recruitRepo.findById(id).orElse(null);
        if (recruitment == null) return Result.fail("引进需求不存在");
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        boolean isOwner = recruitment.getCompany() != null
                && recruitment.getCompany().getUser().getId().equals(user.getId());
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isOwner && !isAdmin) return Result.fail("无权限");
        recruitRepo.deleteById(id);
        return Result.ok();
    }
}
