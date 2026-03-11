package com.talent.platform.controller;

import com.talent.platform.common.Result;
import com.talent.platform.entity.Company;
import com.talent.platform.entity.LearningCertificate;
import com.talent.platform.entity.TalentProfile;
import com.talent.platform.entity.User;
import com.talent.platform.repository.*;
import com.talent.platform.service.BlockchainService;
import com.talent.platform.service.CrawlerService;
import com.talent.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
    private final CompanyRepository companyRepo;
    private final ApiAccessLogRepository apiAccessLogRepo;
    private final CrawlerService crawlerService;
    private final BlockchainBlockRepository blockchainBlockRepo;
    private final LearningCertificateRepository learningCertRepo;
    private final BlockchainService blockchainService;
    private final NotificationService notificationService;

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

    @PutMapping("/company/{id}/audit")
    public Result<?> auditCompany(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Company company = companyRepo.findById(id).orElse(null);
        if (company == null) return Result.fail("企业不存在");
        try {
            company.setAuditStatus(Company.AuditStatus.valueOf(body.get("status").toUpperCase()));
            companyRepo.save(company);
            boolean approved = company.getAuditStatus() == Company.AuditStatus.APPROVED;
            notificationService.notifyCompanyAuditResult(company, approved);
        } catch (Exception e) {
            return Result.fail("审核状态无效");
        }
        return Result.ok();
    }

    @GetMapping("/companies")
    public Result<List<Company>> listCompanies() {
        return Result.ok(companyRepo.findAll());
    }

    @GetMapping("/talents")
    public Result<List<TalentProfile>> listTalents() {
        return Result.ok(talentRepo.findAll());
    }

    @PutMapping("/talents/{id}/featured")
    public Result<?> toggleTalentFeatured(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        TalentProfile talent = talentRepo.findById(id).orElse(null);
        if (talent == null) return Result.fail("人才不存在");
        if (body.containsKey("featured")) {
            boolean featured = (Boolean) body.get("featured");
            if (featured && !Boolean.TRUE.equals(talent.getVisible())) {
                return Result.fail("请先公开该人才，再设置精选");
            }
            talent.setFeatured(featured);
            if (featured) {
                notificationService.notifyTalentFeatured(talent);
            }
        }
        if (body.containsKey("featuredOrder")) {
            if (!Boolean.TRUE.equals(talent.getVisible())) {
                return Result.fail("隐藏状态下不可设置展示顺序");
            }
            talent.setFeaturedOrder(((Number) body.get("featuredOrder")).intValue());
        }
        talentRepo.save(talent);
        return Result.ok();
    }

    @PutMapping("/talents/{id}/visibility")
    public Result<?> toggleTalentVisibility(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        TalentProfile talent = talentRepo.findById(id).orElse(null);
        if (talent == null) return Result.fail("人才不存在");
        if (!body.containsKey("visible")) return Result.fail("缺少可见性参数");
        boolean visible = Boolean.parseBoolean(String.valueOf(body.get("visible")));
        talent.setVisible(visible);
        if (!visible) {
            talent.setFeatured(false);
        }
        talentRepo.save(talent);
        return Result.ok();
    }

    @PutMapping("/company/{id}/visibility")
    public Result<?> toggleCompanyVisibility(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Company company = companyRepo.findById(id).orElse(null);
        if (company == null) return Result.fail("企业不存在");
        if (!body.containsKey("visible")) return Result.fail("缺少可见性参数");
        boolean visible = Boolean.parseBoolean(String.valueOf(body.get("visible")));
        company.setVisible(visible);
        companyRepo.save(company);
        return Result.ok();
    }

@GetMapping("/monitor/api-stats")
    public Result<Map<String, Object>> apiStats() {
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime sevenDaysAgo = todayStart.minusDays(7);
        long totalCalls = apiAccessLogRepo.count();
        long todayCalls = apiAccessLogRepo.countByCreateTimeAfter(todayStart);
        Double avgCost = apiAccessLogRepo.avgCostSince(todayStart);
        long errorCount = apiAccessLogRepo.countErrorsSince(todayStart);
        long successCount = apiAccessLogRepo.countSuccessSince(todayStart);
        List<Object[]> topUrisRaw = apiAccessLogRepo.findTopUris();
        List<Map<String, Object>> topUris = topUrisRaw.stream()
                .limit(10)
                .map(arr -> Map.<String, Object>of("uri", arr[0], "count", ((Number) arr[1]).longValue()))
                .collect(Collectors.toList());

        // 近7天每日调用趋势
        List<Object[]> dailyRaw = apiAccessLogRepo.findDailyCountsSince(sevenDaysAgo);
        List<String> dailyLabels = new ArrayList<>();
        List<Long> dailyValues = new ArrayList<>();
        for (Object[] row : dailyRaw) {
            dailyLabels.add(row[0].toString());
            dailyValues.add(((Number) row[1]).longValue());
        }
        // 今日每小时调用趋势（补齐24小时）
        List<Object[]> hourlyRaw = apiAccessLogRepo.findHourlyCountsSince(todayStart);
        long[] hourlyValues = new long[24];
        for (Object[] row : hourlyRaw) {
            int h = ((Number) row[0]).intValue();
            if (h >= 0 && h < 24) hourlyValues[h] = ((Number) row[1]).longValue();
        }
        List<String> hourlyLabels = new ArrayList<>();
        List<Long> hourlyCounts = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hourlyLabels.add(i + "时");
            hourlyCounts.add(hourlyValues[i]);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalCalls", totalCalls);
        result.put("todayCalls", todayCalls);
        result.put("avgResponseTime", avgCost != null ? avgCost : 0.0);
        result.put("errorCount", errorCount);
        result.put("successCount", successCount);
        result.put("topUris", topUris);
        result.put("dailyTrend", Map.of("labels", dailyLabels, "values", dailyValues));
        result.put("hourlyTrend", Map.of("labels", hourlyLabels, "values", hourlyCounts));
        return Result.ok(result);
    }

    @GetMapping("/crawler/status")
    public Result<Map<String, Object>> crawlerStatus() {
        return Result.ok(crawlerService.getStatus());
    }

    @PostMapping("/crawler/run")
    public Result<Map<String, Object>> runCrawler() {
        return Result.ok(crawlerService.startNewsCrawler());
    }

    /** 区块链管理 - 统计数据 */
    @GetMapping("/blockchain/stats")
    public Result<Map<String, Object>> blockchainStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long blockCount = blockchainBlockRepo.count();
        boolean valid = blockchainService.verifyChain();
        long certCount = learningCertRepo.count();

        stats.put("blockCount", blockCount);
        stats.put("chainValid", valid);
        stats.put("certificateCount", certCount);
        stats.put("chainHeight", blockCount > 0 ? blockCount - 1 : 0); // 排除创世块

        blockchainBlockRepo.findTopByOrderByBlockIndexDesc().ifPresent(block -> {
            stats.put("latestBlockHash", block.getHash());
            stats.put("latestBlockIndex", block.getBlockIndex());
            stats.put("latestBlockTime", block.getTimestamp());
        });

        return Result.ok(stats);
    }

    /** 区块链管理 - 所有存证证书及对应区块 */
    @GetMapping("/blockchain/certificates")
    public Result<List<Map<String, Object>>> blockchainCertificates() {
        List<LearningCertificate> certs = learningCertRepo.findAllWithDetails();
        List<Map<String, Object>> list = new ArrayList<>();
        for (LearningCertificate c : certs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("certificateNo", c.getCertificateNo());
            item.put("userId", c.getUser().getId());
            item.put("username", c.getUser().getUsername());
            item.put("courseId", c.getCourse().getId());
            item.put("courseName", c.getCourse().getTitle());
            item.put("blockHash", c.getBlockHash());
            item.put("issueTime", c.getIssueTime());
            item.put("status", c.getStatus());
            blockchainBlockRepo.findByHash(c.getBlockHash()).ifPresent(block ->
                item.put("blockIndex", block.getBlockIndex())
            );
            list.add(item);
        }
        list.sort((a, b) -> {
            Object t1 = a.get("issueTime");
            Object t2 = b.get("issueTime");
            if (t1 == null || t2 == null) return 0;
            return ((Comparable) t2).compareTo(t1);
        });
        return Result.ok(list);
    }
}
