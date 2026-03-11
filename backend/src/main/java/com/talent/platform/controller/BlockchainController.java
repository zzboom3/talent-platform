package com.talent.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.platform.common.Result;
import com.talent.platform.entity.*;
import com.talent.platform.repository.*;
import com.talent.platform.service.BlockchainService;
import com.talent.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/blockchain")
@RequiredArgsConstructor
public class BlockchainController {

    private final BlockchainService blockchainService;
    private final BlockchainBlockRepository blockRepo;
    private final CourseEnrollmentRepository enrollRepo;
    private final LearningCertificateRepository certRepo;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @GetMapping("/chain")
    public Result<?> getChain() {
        return Result.ok(blockRepo.findAllByOrderByBlockIndexDesc());
    }

    @GetMapping("/verify")
    public Result<?> verify() {
        boolean valid = blockchainService.verifyChain();
        return Result.ok(Map.of("valid", valid, "blockCount", blockRepo.count()));
    }

    @GetMapping("/admin/stats")
    public Result<?> adminStats(@AuthenticationPrincipal UserDetails userDetails) {
        if (!isAdmin(userDetails)) {
            return Result.fail("权限不足");
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        long blockCount = blockRepo.count();
        boolean valid = blockchainService.verifyChain();
        long certCount = certRepo.count();

        stats.put("blockCount", blockCount);
        stats.put("chainValid", valid);
        stats.put("certificateCount", certCount);
        stats.put("chainHeight", blockCount > 0 ? blockCount - 1 : 0);

        blockRepo.findTopByOrderByBlockIndexDesc().ifPresent(block -> {
            stats.put("latestBlockHash", block.getHash());
            stats.put("latestBlockIndex", block.getBlockIndex());
            stats.put("latestBlockTime", block.getTimestamp());
        });

        return Result.ok(stats);
    }

    @GetMapping("/admin/certificates")
    public Result<?> adminCertificates(@AuthenticationPrincipal UserDetails userDetails) {
        if (!isAdmin(userDetails)) {
            return Result.fail("权限不足");
        }

        List<LearningCertificate> certs = certRepo.findAllWithDetails();
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
            blockRepo.findByHash(c.getBlockHash()).ifPresent(block ->
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

    @GetMapping("/block/{hash}")
    public Result<?> getBlock(@PathVariable String hash) {
        return blockRepo.findByHash(hash)
                .map(Result::ok)
                .orElse(Result.fail("区块不存在"));
    }

    @PostMapping("/certify")
    public Result<?> certify(@RequestBody Map<String, Long> body,
                             @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        Long enrollmentId = body.get("enrollmentId");
        if (enrollmentId == null) return Result.fail("缺少 enrollmentId");

        CourseEnrollment enrollment = enrollRepo.findById(enrollmentId).orElse(null);
        if (enrollment == null) return Result.fail("报名记录不存在");
        if (enrollment.getProgress() < 100 || !"COMPLETED".equals(enrollment.getStatus())) {
            return Result.fail("课程尚未完成，无法颁发证书");
        }
        if (certRepo.existsByUserIdAndCourseId(user.getId(), enrollment.getCourse().getId())) {
            return Result.fail("该课程证书已存在");
        }

        String certNo = "CERT-" + System.currentTimeMillis();
        try {
            Map<String, Object> certData = new LinkedHashMap<>();
            certData.put("userId", user.getId());
            certData.put("username", user.getUsername());
            certData.put("courseId", enrollment.getCourse().getId());
            certData.put("courseName", enrollment.getCourse().getTitle());
            certData.put("certificateNo", certNo);
            certData.put("completionTime", LocalDateTime.now().toString());

            BlockchainBlock block = blockchainService.addBlock(objectMapper.writeValueAsString(certData));

            LearningCertificate cert = new LearningCertificate();
            cert.setUser(user);
            cert.setCourse(enrollment.getCourse());
            cert.setCertificateNo(certNo);
            cert.setBlockHash(block.getHash());
            certRepo.save(cert);
            notificationService.notifyCertificateIssued(user, enrollment.getCourse().getTitle());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("certificate", cert);
            result.put("block", block);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail("证书生成失败: " + e.getMessage());
        }
    }

    @GetMapping("/certificates")
    public Result<?> myCertificates(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        return Result.ok(certRepo.findByUser(user));
    }

    @GetMapping("/certificates/{id}")
    public Result<?> getCertificate(@PathVariable Long id) {
        LearningCertificate cert = certRepo.findById(id).orElse(null);
        if (cert == null) return Result.fail("证书不存在");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("certificate", cert);
        blockRepo.findByHash(cert.getBlockHash()).ifPresent(block -> result.put("block", block));
        return Result.ok(result);
    }

    private boolean isAdmin(UserDetails userDetails) {
        return userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
    }
}
