package com.talent.platform.controller;

import com.talent.platform.common.Result;
import com.talent.platform.entity.Company;
import com.talent.platform.entity.Company.AuditStatus;
import com.talent.platform.entity.User;
import com.talent.platform.repository.CompanyRepository;
import com.talent.platform.repository.UserRepository;
import com.talent.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyRepository companyRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;

    @GetMapping("/my")
    public Result<Company> getMy(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        return companyRepo.findByUser(user).map(Result::ok).orElse(Result.fail("尚未创建企业信息"));
    }

    @PostMapping
    public Result<Company> create(@RequestBody Company company,
                                  @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        if (companyRepo.findByUser(user).isPresent()) return Result.fail("企业信息已存在");
        company.setUser(user);
        company = companyRepo.save(company);
        if (company.getAuditStatus() == AuditStatus.PENDING) {
            notificationService.notifyCompanyPendingAudit(company);
        }
        return Result.ok(company);
    }

    @PutMapping("/{id}")
    public Result<Company> update(@PathVariable Long id, @RequestBody Company updated,
                                  @AuthenticationPrincipal UserDetails userDetails) {
        Company company = companyRepo.findById(id).orElse(null);
        if (company == null) return Result.fail("企业不存在");
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        if (!company.getUser().getId().equals(user.getId())) return Result.fail("无权限");
        company.setCompanyName(updated.getCompanyName());
        company.setIndustry(updated.getIndustry());
        company.setDescription(updated.getDescription());
        company.setContactEmail(updated.getContactEmail());
        company.setContactPhone(updated.getContactPhone());
        company.setScale(updated.getScale());
        company.setAddress(updated.getAddress());
        company.setWebsite(updated.getWebsite());
        company.setLogoUrl(updated.getLogoUrl());
        boolean resubmitted = false;
        if (company.getAuditStatus() == AuditStatus.REJECTED) {
            company.setAuditStatus(AuditStatus.PENDING);
            resubmitted = true;
        }
        company = companyRepo.save(company);
        if (resubmitted) {
            notificationService.notifyCompanyPendingAudit(company);
        }
        return Result.ok(company);
    }
}
