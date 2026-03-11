package com.talent.platform.service;

import com.talent.platform.entity.BlockchainBlock;
import com.talent.platform.entity.News;
import com.talent.platform.entity.TalentProfile;
import com.talent.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserRepository userRepository;
    private final TalentProfileRepository talentProfileRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final NewsRepository newsRepository;
    private final BlockchainBlockRepository blockchainBlockRepository;
    private final LearningCertificateRepository learningCertificateRepository;
    private final ApiAccessLogRepository apiAccessLogRepository;
    private final CareerAssessmentRepository careerAssessmentRepository;

    public Map<String, Object> getPublicStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("users", userRepository.count());
        stats.put("talents", talentProfileRepository.countByVisibleTrue());
        stats.put("jobs", jobRepository.findAll().stream()
                .filter(job -> job.getCompany() != null
                        && job.getCompany().getAuditStatus() == com.talent.platform.entity.Company.AuditStatus.APPROVED
                        && Boolean.TRUE.equals(job.getCompany().getVisible()))
                .count());
        stats.put("courses", courseRepository.count());
        stats.put("news", newsRepository.count());
        stats.put("applications", jobApplicationRepository.count());
        return stats;
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        Map<String, Long> applicationStatusStats = getApplicationStatusStats();
        Map<String, Object> monthlyTrend = getMonthlyTrend();
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<TalentProfile> talents = talentProfileRepository.findAll();

        stats.put("users", userRepository.count());
        stats.put("talents", talentProfileRepository.count());
        stats.put("companies", companyRepository.count());
        stats.put("jobs", jobRepository.count());
        stats.put("openJobs", jobRepository.countByStatus("OPEN"));
        stats.put("courses", courseRepository.count());
        stats.put("news", newsRepository.count());
        stats.put("applications", jobApplicationRepository.count());
        stats.put("policyCount", newsRepository.countByCategory(News.Category.POLICY));
        stats.put("announcementCount", newsRepository.countByCategory(News.Category.ANNOUNCE));
        stats.put("monthlyTrend", monthlyTrend);
        stats.put("applicationStatusStats", applicationStatusStats);

        Map<String, Long> talentsByCity = talents.stream()
                .filter(t -> t.getCity() != null && !t.getCity().isBlank())
                .collect(Collectors.groupingBy(TalentProfile::getCity, Collectors.counting()));
        talentsByCity = sortCountMapDescending(talentsByCity);
        stats.put("talentsByCity", talentsByCity);
        stats.put("coveredCities", talentsByCity.size());

        Map<String, Long> skillsFrequency = talents.stream()
                .map(TalentProfile::getSkills)
                .filter(s -> s != null && !s.isBlank())
                .flatMap(s -> Arrays.stream(s.split("[,，/|\\s]+")))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.groupingBy(String::toLowerCase, Collectors.counting()));
        Map<String, Long> topSkillsMap = sortCountMapDescending(skillsFrequency).entrySet().stream()
                .limit(20)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        stats.put("skillsFrequency", topSkillsMap);
        stats.put("majorsFrequency", topCountMap(talents.stream().map(TalentProfile::getMajor).toList(), 8));
        stats.put("workYearsDistribution", topCountMap(talents.stream().map(TalentProfile::getWorkYears).toList(), 8));
        stats.put("expectedPositions", topCountMap(talents.stream().map(TalentProfile::getExpectedPosition).toList(), 8));
        stats.put("profileCompleteness", buildProfileCompletenessStats(talents));

        Map<String, Object> blockchainStats = new LinkedHashMap<>();
        blockchainStats.put("blockCount", blockchainBlockRepository.count());
        blockchainBlockRepository.findTopByOrderByBlockIndexDesc()
                .map(BlockchainBlock::getHash)
                .ifPresent(h -> blockchainStats.put("latestBlockHash", h));
        stats.put("blockchainStats", blockchainStats);

        stats.put("certificateCount", learningCertificateRepository.count());
        stats.put("assessmentCount", careerAssessmentRepository.count());
        stats.put("resourceStats", buildResourceStats());
        stats.put("recentActivity", buildRecentActivityStats(sevenDaysAgo));
        stats.put("serviceResults", buildServiceResultStats(applicationStatusStats, talentsByCity.size()));

        return stats;
    }

    public Map<String, Object> getMonthlyTrend() {
        List<YearMonth> months = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            months.add(current.minusMonths(i));
        }

        LocalDateTime startTime = months.get(0).atDay(1).atStartOfDay();
        Map<YearMonth, Long> registrationCounts = toMonthCountMap(userRepository.countMonthlyRegistrations(startTime));
        Map<YearMonth, Long> applicationCounts = toMonthCountMap(jobApplicationRepository.countMonthlyApplications(startTime));
        Map<YearMonth, Long> enrollmentCounts = toMonthCountMap(courseEnrollmentRepository.countMonthlyEnrollments(startTime));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M月");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("months", months.stream().map(month -> month.atDay(1).format(formatter)).toList());
        result.put("registrations", months.stream().map(month -> registrationCounts.getOrDefault(month, 0L)).toList());
        result.put("applications", months.stream().map(month -> applicationCounts.getOrDefault(month, 0L)).toList());
        result.put("enrollments", months.stream().map(month -> enrollmentCounts.getOrDefault(month, 0L)).toList());
        return result;
    }

    public Map<String, Long> getApplicationStatusStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("PENDING", 0L);
        stats.put("ACCEPTED", 0L);
        stats.put("REJECTED", 0L);
        for (Object[] row : jobApplicationRepository.countByStatus()) {
            stats.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return stats;
    }

    private Map<YearMonth, Long> toMonthCountMap(List<Object[]> rows) {
        Map<YearMonth, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();
            result.put(YearMonth.of(year, month), count);
        }
        return result;
    }

    private Map<String, Object> buildResourceStats() {
        Map<String, Object> resourceStats = new LinkedHashMap<>();
        resourceStats.put("talents", talentProfileRepository.count());
        resourceStats.put("companies", companyRepository.count());
        resourceStats.put("openJobs", jobRepository.countByStatus("OPEN"));
        resourceStats.put("courses", courseRepository.count());
        resourceStats.put("policies", newsRepository.countByCategory(News.Category.POLICY));
        resourceStats.put("news", newsRepository.countByCategory(News.Category.NEWS));
        resourceStats.put("announcements", newsRepository.countByCategory(News.Category.ANNOUNCE));
        return resourceStats;
    }

    private Map<String, Object> buildRecentActivityStats(LocalDateTime since) {
        Map<String, Object> recentActivity = new LinkedHashMap<>();
        recentActivity.put("recentJobs", jobRepository.countByCreateTimeAfter(since));
        recentActivity.put("recentPolicies", newsRepository.countByCategoryAndPublishTimeAfter(News.Category.POLICY, since));
        recentActivity.put("recentNews", newsRepository.countByCategoryAndPublishTimeAfter(News.Category.NEWS, since));
        recentActivity.put("recentAnnouncements", newsRepository.countByCategoryAndPublishTimeAfter(News.Category.ANNOUNCE, since));
        return recentActivity;
    }

    private Map<String, Object> buildServiceResultStats(Map<String, Long> applicationStatusStats, int coveredCities) {
        Map<String, Object> serviceResults = new LinkedHashMap<>();
        serviceResults.put("applications", jobApplicationRepository.count());
        serviceResults.put("acceptedApplications", applicationStatusStats.getOrDefault("ACCEPTED", 0L));
        serviceResults.put("pendingApplications", applicationStatusStats.getOrDefault("PENDING", 0L));
        serviceResults.put("certificates", learningCertificateRepository.count());
        serviceResults.put("assessments", careerAssessmentRepository.count());
        serviceResults.put("learningParticipants", courseEnrollmentRepository.count());
        serviceResults.put("coveredCities", coveredCities);
        return serviceResults;
    }

    private Map<String, Long> topCountMap(List<String> values, int limit) {
        Map<String, Long> countMap = values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.groupingBy(value -> value, Collectors.counting()));

        return sortCountMapDescending(countMap).entrySet().stream()
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Map<String, Long> buildProfileCompletenessStats(List<TalentProfile> talents) {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("基础档案", 0L);
        result.put("较完整", 0L);
        result.put("高完整度", 0L);

        for (TalentProfile talent : talents) {
            int completeness = profileCompleteness(talent);
            if (completeness >= 7) {
                result.put("高完整度", result.get("高完整度") + 1);
            } else if (completeness >= 4) {
                result.put("较完整", result.get("较完整") + 1);
            } else {
                result.put("基础档案", result.get("基础档案") + 1);
            }
        }
        return result;
    }

    private int profileCompleteness(TalentProfile talent) {
        int score = 0;
        if (hasText(talent.getRealName())) score++;
        if (hasText(talent.getSkills())) score++;
        if (hasText(talent.getEducation())) score++;
        if (hasText(talent.getMajor())) score++;
        if (hasText(talent.getWorkYears())) score++;
        if (hasText(talent.getExpectedPosition())) score++;
        if (hasText(talent.getExperience())) score++;
        if (hasText(talent.getProjectExperience())) score++;
        if (hasText(talent.getSelfIntroduction())) score++;
        return score;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Map<String, Long> sortCountMapDescending(Map<String, Long> source) {
        return source.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }
}
