package com.talent.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.platform.common.Result;
import com.talent.platform.entity.*;
import com.talent.platform.entity.Company.AuditStatus;
import com.talent.platform.repository.*;
import com.talent.platform.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final UserRepository userRepo;
    private final TalentProfileRepository talentRepo;
    private final JobRepository jobRepo;
    private final CourseRepository courseRepo;
    private final CourseEnrollmentRepository enrollRepo;
    private final CareerAssessmentRepository assessmentRepo;
    private final ObjectMapper objectMapper;

    @PostMapping("/match")
    public Result<?> match(@AuthenticationPrincipal UserDetails userDetails,
                           @RequestBody Map<String, Object> body) {
        TalentProfile talent = resolveTalentProfile(userDetails, body);
        String query = mergeText(
                stringValue(body.get("query")),
                stringValue(body.get("skills")),
                stringValue(body.get("experience")),
                stringValue(body.get("expectedPosition")),
                stringValue(body.get("projectExperience"))
        );
        String profileSummary = buildTalentSummary(talent);
        if (query.isBlank() && profileSummary.isBlank()) {
            return Result.fail("请输入匹配描述，或先完善人才档案");
        }

        List<Job> jobs = jobRepo.findAll().stream()
                .filter(j -> "OPEN".equals(j.getStatus())
                        && j.getCompany() != null
                        && j.getCompany().getAuditStatus() == Company.AuditStatus.APPROVED
                        && Boolean.TRUE.equals(j.getCompany().getVisible()))
                .collect(Collectors.toList());

        List<Map<String, String>> jobList = jobs.stream().map(j -> {
            Map<String, String> m = new HashMap<>();
            m.put("jobId", j.getId().toString());
            m.put("title", j.getTitle());
            Company c = j.getCompany();
            m.put("companyName", c != null ? safe(c.getCompanyName()) : "");
            m.put("industry", c != null ? safe(c.getIndustry()) : "");
            m.put("scale", c != null ? safe(c.getScale()) : "");
            m.put("companyDesc", c != null ? safe(c.getDescription()) : "");
            m.put("requirements", j.getRequirements() != null ? j.getRequirements() : "");
            m.put("description", j.getDescription() != null ? j.getDescription() : "");
            m.put("city", j.getCity() != null ? j.getCity() : "");
            m.put("salaryRange", j.getSalaryRange() != null ? j.getSalaryRange() : "");
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = aiService.matchTalentToJobs(query, profileSummary, jobList);
        enrichMatchResults(result, jobs);
        return Result.ok(result);
    }

    @SuppressWarnings("unchecked")
    private void enrichMatchResults(Map<String, Object> result, List<Job> jobs) {
        if (result == null || !(result.get("matches") instanceof List<?> rawMatches)) {
            return;
        }

        Map<Long, Job> jobsById = jobs.stream().collect(Collectors.toMap(Job::getId, job -> job));
        List<Map<String, Object>> enrichedMatches = new ArrayList<>();

        for (Object rawMatch : rawMatches) {
            if (!(rawMatch instanceof Map<?, ?> rawMap)) {
                continue;
            }

            Map<String, Object> match = new LinkedHashMap<>();
            rawMap.forEach((key, value) -> match.put(String.valueOf(key), value));

            Long jobId = parseJobId(match.get("jobId"));
            if (jobId == null) {
                continue;
            }

            Job job = jobsById.get(jobId);
            if (job == null) {
                continue;
            }

            match.put("jobId", jobId);
            match.put("title", job.getTitle());
            match.put("companyName", job.getCompany() != null ? job.getCompany().getCompanyName() : "");
            match.put("city", job.getCity());
            match.put("salaryRange", job.getSalaryRange());
            enrichedMatches.add(match);
        }

        result.put("matches", enrichedMatches);
    }

    private Long parseJobId(Object jobIdValue) {
        if (jobIdValue == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(jobIdValue));
        } catch (Exception ignored) {
            return null;
        }
    }

    @PostMapping("/match-talents")
    public Result<?> matchTalents(@AuthenticationPrincipal UserDetails userDetails,
                                  @RequestBody Map<String, Object> body) {
        Job selectedJob = null;
        if (body.containsKey("jobId")) {
            Long jobId = parseJobId(body.get("jobId"));
            if (jobId == null) return Result.fail("岗位ID无效");
            selectedJob = jobRepo.findById(jobId).orElse(null);
            if (selectedJob == null) return Result.fail("岗位不存在");
        }

        String query = mergeText(
                stringValue(body.get("query")),
                stringValue(body.get("title")),
                stringValue(body.get("requirements")),
                stringValue(body.get("keyword"))
        );
        String jobSummary = buildJobSummary(selectedJob);
        if (query.isBlank() && jobSummary.isBlank()) {
            return Result.fail("请输入岗位需求，或选择一个岗位");
        }

        List<TalentProfile> talents = talentRepo.findAll().stream()
                .filter(t -> t.getStatus() == null || "ACTIVE".equalsIgnoreCase(t.getStatus()))
                .filter(t -> Boolean.TRUE.equals(t.getVisible()))
                .collect(Collectors.toList());

        List<Map<String, String>> talentList = talents.stream().map(t -> {
            Map<String, String> m = new HashMap<>();
            m.put("talentId", t.getId().toString());
            m.put("realName", t.getRealName() != null ? t.getRealName() : "");
            m.put("username", t.getUser() != null && t.getUser().getUsername() != null ? t.getUser().getUsername() : "");
            m.put("skills", t.getSkills() != null ? t.getSkills() : "");
            m.put("education", t.getEducation() != null ? t.getEducation() : "");
            m.put("gender", t.getGender() != null ? t.getGender() : "");
            m.put("graduationSchool", t.getGraduationSchool() != null ? t.getGraduationSchool() : "");
            m.put("major", t.getMajor() != null ? t.getMajor() : "");
            m.put("workYears", t.getWorkYears() != null ? t.getWorkYears() : "");
            m.put("expectedPosition", t.getExpectedPosition() != null ? t.getExpectedPosition() : "");
            m.put("expectedSalary", t.getExpectedSalary() != null ? t.getExpectedSalary() : "");
            m.put("experience", t.getExperience() != null ? t.getExperience() : "");
            m.put("projectExperience", t.getProjectExperience() != null ? t.getProjectExperience() : "");
            m.put("selfIntroduction", t.getSelfIntroduction() != null ? t.getSelfIntroduction() : "");
            m.put("certificates", t.getCertificates() != null ? t.getCertificates() : "");
            m.put("city", t.getCity() != null ? t.getCity() : "");
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = aiService.matchJobToTalents(query, jobSummary, talentList);
        enrichTalentMatchResults(result, talents);
        return Result.ok(result);
    }

    private TalentProfile resolveTalentProfile(UserDetails userDetails, Map<String, Object> body) {
        if (body.containsKey("talentId")) {
            Long talentId = parseTalentId(body.get("talentId"));
            if (talentId == null) {
                return null;
            }
            return talentRepo.findById(talentId).orElse(null);
        }

        boolean useProfile = !body.containsKey("useProfile") || Boolean.parseBoolean(String.valueOf(body.get("useProfile")));
        if (!useProfile || userDetails == null) {
            return null;
        }

        return userRepo.findByUsername(userDetails.getUsername())
                .flatMap(talentRepo::findByUser)
                .orElse(null);
    }

    private String buildTalentSummary(TalentProfile talent) {
        if (talent == null) {
            return "";
        }
        return mergeText(
                talent.getRealName(),
                talent.getSkills(),
                talent.getEducation(),
                talent.getGender(),
                talent.getGraduationSchool(),
                talent.getMajor(),
                talent.getWorkYears(),
                talent.getExpectedPosition(),
                talent.getExpectedSalary(),
                talent.getExperience(),
                talent.getProjectExperience(),
                talent.getSelfIntroduction(),
                talent.getCertificates(),
                talent.getCity()
        );
    }

    private String buildJobSummary(Job job) {
        if (job == null) {
            return "";
        }
        Company c = job.getCompany();
        return mergeText(
                job.getTitle(),
                job.getRequirements(),
                job.getDescription(),
                job.getCity(),
                job.getSalaryRange(),
                c != null ? c.getCompanyName() : "",
                c != null ? c.getIndustry() : "",
                c != null ? c.getScale() : "",
                c != null ? c.getDescription() : ""
        );
    }

    private String mergeText(String... parts) {
        return Arrays.stream(parts)
                .filter(Objects::nonNull)
                .map(this::stringValue)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("；"));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    @SuppressWarnings("unchecked")
    private void enrichTalentMatchResults(Map<String, Object> result, List<TalentProfile> talents) {
        if (result == null || !(result.get("matches") instanceof List<?> rawMatches)) {
            return;
        }

        Map<Long, TalentProfile> talentsById = talents.stream().collect(Collectors.toMap(TalentProfile::getId, t -> t));
        List<Map<String, Object>> enrichedMatches = new ArrayList<>();

        for (Object rawMatch : rawMatches) {
            if (!(rawMatch instanceof Map<?, ?> rawMap)) {
                continue;
            }

            Map<String, Object> match = new LinkedHashMap<>();
            rawMap.forEach((key, value) -> match.put(String.valueOf(key), value));

            Long talentId = parseTalentId(match.get("talentId"));
            if (talentId == null) {
                continue;
            }

            TalentProfile talent = talentsById.get(talentId);
            if (talent == null) {
                continue;
            }

            match.put("talentId", talentId);
            match.put("realName", talent.getRealName());
            match.put("skills", talent.getSkills());
            match.put("education", talent.getEducation());
            match.put("gender", talent.getGender());
            match.put("graduationSchool", talent.getGraduationSchool());
            match.put("major", talent.getMajor());
            match.put("workYears", talent.getWorkYears());
            match.put("expectedPosition", talent.getExpectedPosition());
            match.put("expectedSalary", talent.getExpectedSalary());
            match.put("experience", talent.getExperience());
            match.put("projectExperience", talent.getProjectExperience());
            match.put("selfIntroduction", talent.getSelfIntroduction());
            match.put("certificates", talent.getCertificates());
            match.put("city", talent.getCity());
            match.put("avatarUrl", talent.getAvatarUrl());
            match.put("username", talent.getUser() != null ? talent.getUser().getUsername() : "");
            enrichedMatches.add(match);
        }

        result.put("matches", enrichedMatches);
    }

    private Long parseTalentId(Object talentIdValue) {
        if (talentIdValue == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(talentIdValue));
        } catch (Exception ignored) {
            return null;
        }
    }

    @PostMapping("/recommend-courses")
    public Result<?> recommendCourses(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        TalentProfile talent = talentRepo.findByUser(user).orElse(null);
        String profileSummary = buildTalentSummary(talent);

        List<CourseEnrollment> enrollments = enrollRepo.findByUser(user);
        List<String> completed = enrollments.stream()
                .filter(e -> "COMPLETED".equals(e.getStatus()))
                .map(e -> e.getCourse().getTitle())
                .collect(Collectors.toList());

        List<Course> allCourses = courseRepo.findAll();
        List<Map<String, String>> courseList = allCourses.stream().map(c -> {
            Map<String, String> m = new HashMap<>();
            m.put("courseId", c.getId().toString());
            m.put("title", c.getTitle());
            m.put("description", c.getDescription() != null ? c.getDescription() : "");
            m.put("category", c.getCategory() != null ? c.getCategory() : "");
            m.put("teacher", c.getTeacher() != null ? c.getTeacher() : "");
            m.put("duration", c.getDuration() != null ? c.getDuration().toString() : "");
            m.put("chapterCount", c.getChapterCount() != null ? c.getChapterCount().toString() : "");
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = aiService.recommendCourses(profileSummary, completed, courseList);
        enrichCourseRecommendations(result, allCourses);
        return Result.ok(result);
    }

    @SuppressWarnings("unchecked")
    private void enrichCourseRecommendations(Map<String, Object> result, List<Course> courses) {
        if (result == null || !(result.get("recommendations") instanceof List<?> rawRecommendations)) {
            return;
        }

        Map<Long, Course> coursesById = courses.stream().collect(Collectors.toMap(Course::getId, course -> course));
        List<Map<String, Object>> enrichedRecommendations = new ArrayList<>();

        for (Object rawRecommendation : rawRecommendations) {
            if (!(rawRecommendation instanceof Map<?, ?> rawMap)) {
                continue;
            }

            Map<String, Object> recommendation = new LinkedHashMap<>();
            rawMap.forEach((key, value) -> recommendation.put(String.valueOf(key), value));

            Long courseId = parseCourseId(recommendation.get("courseId"));
            if (courseId == null) {
                continue;
            }

            Course course = coursesById.get(courseId);
            if (course == null) {
                continue;
            }

            recommendation.put("courseId", courseId);
            recommendation.put("title", course.getTitle());
            recommendation.put("teacher", course.getTeacher());
            recommendation.put("category", course.getCategory());
            recommendation.put("coverUrl", course.getCoverUrl());
            recommendation.put("description", course.getDescription());
            recommendation.put("duration", course.getDuration());
            recommendation.put("chapterCount", course.getChapterCount());
            enrichedRecommendations.add(recommendation);
        }

        result.put("recommendations", enrichedRecommendations);
    }

    private Long parseCourseId(Object courseIdValue) {
        if (courseIdValue == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(courseIdValue));
        } catch (Exception ignored) {
            return null;
        }
    }

    @PostMapping("/career-assessment")
    public Result<?> careerAssessment(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        TalentProfile talent = talentRepo.findByUser(user).orElse(null);
        if (talent == null) return Result.fail("请先创建人才档案");

        Map<String, Object> result = aiService.assessCareer(
                talent.getRealName(),
                talent.getSkills(),
                talent.getEducation(),
                talent.getExperience(),
                talent.getMajor(),
                talent.getWorkYears(),
                talent.getExpectedPosition(),
                talent.getProjectExperience(),
                talent.getSelfIntroduction(),
                talent.getCertificates(),
                talent.getGender(),
                talent.getGraduationSchool(),
                talent.getExpectedSalary(),
                talent.getCity());

        try {
            CareerAssessment assessment = new CareerAssessment();
            assessment.setUser(user);
            assessment.setRadarData(objectMapper.writeValueAsString(result.get("radarData")));
            assessment.setAssessmentReport(result.containsKey("report") ? result.get("report").toString() : "");
            assessment.setSuggestions(objectMapper.writeValueAsString(result.get("suggestions")));
            Object score = result.get("overallScore");
            assessment.setOverallScore(score instanceof Number ? ((Number) score).intValue() : 0);
            assessmentRepo.save(assessment);
            result.put("assessmentId", assessment.getId());
        } catch (Exception e) {
            result.put("saveError", "评估结果保存失败");
        }

        return Result.ok(result);
    }

    @GetMapping("/assessments")
    public Result<?> assessmentHistory(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        return Result.ok(assessmentRepo.findByUserOrderByCreateTimeDesc(user));
    }

    @GetMapping("/assessments/{id}")
    public Result<?> getAssessment(@PathVariable Long id) {
        return assessmentRepo.findById(id)
                .map(Result::ok)
                .orElse(Result.fail("评估记录不存在"));
    }

    /** AI 智能推荐人才展示（管理员专用，走 /api/ai/admin/* 与区块链管理同模式） */
    @GetMapping("/admin/recommend-showcase")
    public Result<?> recommendShowcase(@AuthenticationPrincipal UserDetails userDetails,
                                       @RequestParam(defaultValue = "10") int limit) {
        if (!isAdmin(userDetails)) {
            return Result.fail("权限不足");
        }
        List<TalentProfile> all = talentRepo.findAll();
        List<Map<String, String>> talentsForAi = all.stream()
                .map(t -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("talentId", t.getId().toString());
                    m.put("id", t.getId().toString());
                    m.put("realName", t.getRealName() != null ? t.getRealName() : "");
                    m.put("gender", t.getGender() != null ? t.getGender() : "");
                    m.put("skills", t.getSkills() != null ? t.getSkills() : "");
                    m.put("education", t.getEducation() != null ? t.getEducation() : "");
                    m.put("graduationSchool", t.getGraduationSchool() != null ? t.getGraduationSchool() : "");
                    m.put("major", t.getMajor() != null ? t.getMajor() : "");
                    m.put("workYears", t.getWorkYears() != null ? t.getWorkYears() : "");
                    m.put("expectedPosition", t.getExpectedPosition() != null ? t.getExpectedPosition() : "");
                    m.put("expectedSalary", t.getExpectedSalary() != null ? t.getExpectedSalary() : "");
                    m.put("experience", t.getExperience() != null ? t.getExperience() : "");
                    m.put("projectExperience", t.getProjectExperience() != null ? t.getProjectExperience() : "");
                    m.put("selfIntroduction", t.getSelfIntroduction() != null ? t.getSelfIntroduction() : "");
                    m.put("certificates", t.getCertificates() != null ? t.getCertificates() : "");
                    m.put("city", t.getCity() != null ? t.getCity() : "");
                    return m;
                })
                .collect(Collectors.toList());
        Map<String, Object> result;
        try {
            result = aiService.recommendShowcaseTalents(talentsForAi, limit);
        } catch (Exception e) {
            result = new HashMap<>();
            result.put("recommendations", List.of());
            result.put("error", e.getMessage() != null ? e.getMessage() : "AI 服务调用失败");
        }
        return Result.ok(result);
    }

    private boolean isAdmin(UserDetails userDetails) {
        return userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
