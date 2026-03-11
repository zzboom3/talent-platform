package com.talent.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private static final String MODEL = "deepseek-chat";
    private static final double TEMPERATURE = 0.7;
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{IsHan}A-Za-z0-9+#.]{2,}");
    private static final int SHORTLIST_LIMIT = 200;
    private static final int RESULT_LIMIT = 12;
    private static final int MIN_SHORTLIST_SCORE = 18;
    private static final int MIN_FINAL_SCORE = 50;

    private final ObjectMapper objectMapper;

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    private String callDeepSeek(String systemMessage, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("DeepSeek API key is not configured");
            throw new IllegalStateException("DeepSeek API key is not configured. Please set DEEPSEEK_API_KEY.");
        }

        String url = baseUrl.endsWith("/") ? baseUrl + "v1/chat/completions" : baseUrl + "/v1/chat/completions";

        Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "messages", List.of(
                        Map.of("role", "system", "content", systemMessage),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", TEMPERATURE
        );

        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(url)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            Map<String, Object> response = webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null) {
                throw new IllegalStateException("DeepSeek API returned empty response");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new IllegalStateException("DeepSeek API response has no choices");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> choice = (Map<String, Object>) choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            if (message == null) {
                throw new IllegalStateException("DeepSeek API response has no message");
            }

            String content = (String) message.get("content");
            return content != null ? content : "";
        } catch (WebClientResponseException e) {
            log.error("DeepSeek API error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("DeepSeek API call failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("DeepSeek API call failed", e);
            throw new IllegalStateException("DeepSeek API call failed: " + e.getMessage());
        }
    }

    private String extractJsonFromResponse(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }
        String trimmed = response.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return trimmed;
        }
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return trimmed;
    }

    // ==================== 人才→岗位匹配 ====================

    public Map<String, Object> matchTalentToJobs(String freeTextQuery, String talentProfileSummary, List<Map<String, String>> jobs) {
        String normalizedQuery = mergeText(freeTextQuery, talentProfileSummary);
        List<CandidateScore> shortlist = buildAiCandidatePool(jobs, "jobId", this::buildJobSearchText);
        if (shortlist.isEmpty()) {
            return buildEmptyResult("没有可匹配的岗位数据");
        }

        String systemMessage = "你是一位资深招聘顾问，需要把用户的自然语言求职描述映射到最合适的岗位。\n"
                + "核心原则：完全依赖你对自然语言的深度理解来做语义匹配，禁止退化为关键词字面匹配。\n"
                + "用户的输入可能是任何形式——正式描述、口语化表达、模糊意向、反问、省略、甚至只有几个词。\n"
                + "无论输入是什么，你都必须充分理解其中所有可能的隐含信息（包括但不限于：岗位方向、技能、城市、行业、薪资、\n"
                + "企业规模偏好、学历背景、经验阶段、工作节奏、个人诉求，以及任何你能推断出的求职意图），\n"
                + "并结合候选岗位的全部信息（标题、企业名称、行业、规模、城市、薪资、要求、描述等）进行综合判断。\n"
                + "你需要综合「用户自由输入」和「人才档案补充」来判断，但当档案补充为空时，也必须仅根据自由输入完成合理匹配。\n"
                + "即使用户输入非常口语化、模糊，甚至与候选岗位整体关联较弱，你也必须先对候选岗位完成分析、排序和打分，而不是跳过判断。\n"
                + "你只能从候选列表中选择岗位。请对你认为有意义的候选返回分数与理由；如果所有候选都明显不相关，可以返回空数组。\n"
                + "只返回匹配度 >= 50 的结果，并按分数从高到低排序，最多返回前 " + RESULT_LIMIT + " 个。\n"
                + "请严格返回 JSON，格式为：\n"
                + "{\"matches\":[{\"jobId\":\"1\",\"score\":88,\"reason\":\"不超过40字的匹配理由\"}]}\n"
                + "不要输出任何额外说明。";

        StringBuilder prompt = new StringBuilder();
        prompt.append("用户自由输入：").append(safe(freeTextQuery)).append("\n");
        prompt.append("人才档案补充：").append(safe(talentProfileSummary)).append("\n\n");
        prompt.append("候选岗位（已由系统做过首轮召回，请继续做语义精排）：\n");
        appendCandidates(prompt, shortlist, "jobId", item ->
                "- jobId: " + item.id()
                + "\n  标题: " + safe(item.payload().get("title"))
                + "\n  企业: " + safe(item.payload().get("companyName"))
                + "\n  行业: " + safe(item.payload().get("industry"))
                + "\n  规模: " + safe(item.payload().get("scale"))
                + "\n  企业简介: " + safe(item.payload().get("companyDesc"))
                + "\n  城市: " + safe(item.payload().get("city"))
                + "\n  薪资: " + safe(item.payload().get("salaryRange"))
                + "\n  要求: " + safe(item.payload().get("requirements"))
                + "\n  描述: " + safe(item.payload().get("description"))
                + "\n  系统召回提示: " + safe(item.reason()) + "\n"
        );

        return rerankWithAi(systemMessage, prompt.toString(), shortlist, "jobId");
    }

    // ==================== 岗位→人才匹配 ====================

    public Map<String, Object> matchJobToTalents(String freeTextQuery, String jobSummary, List<Map<String, String>> talents) {
        String normalizedQuery = mergeText(freeTextQuery, jobSummary);
        List<CandidateScore> shortlist = buildAiCandidatePool(talents, "talentId", this::buildTalentSearchText);
        if (shortlist.isEmpty()) {
            return buildEmptyResult("没有可匹配的人才数据");
        }

        String systemMessage = "你是一位资深招聘顾问，需要把用户的自然语言招聘需求映射到最合适的人才。\n"
                + "核心原则：完全依赖你对自然语言的深度理解来做语义匹配，禁止退化为关键词字面匹配。\n"
                + "用户的输入可能是任何形式——正式的岗位描述、口语化表达、模糊意向、省略、甚至只有几个关键词。\n"
                + "无论输入是什么，你都必须充分理解其中所有可能的隐含信息（包括但不限于：岗位方向、技能要求、城市、行业背景、\n"
                + "学历要求、毕业院校偏好、经验阶段、软技能、性别偏好，以及任何你能推断出的招聘意图），\n"
                + "并结合候选人才的全部信息（姓名、性别、技能、学历、毕业院校、专业、工作年限、期望岗位、期望薪资、经验、项目经历、个人简介、证书成果、城市等）进行综合判断。\n"
                + "你需要综合「岗位自由输入」和「岗位补充信息」来判断，但当岗位补充信息为空时，也必须仅根据自由输入完成合理匹配。\n"
                + "即使用户输入非常口语化、模糊，甚至与候选人才整体关联较弱，你也必须先对候选人才完成分析、排序和打分，而不是跳过判断。\n"
                + "你只能从候选列表中选择人才。请对你认为有意义的候选返回分数与理由；如果所有候选都明显不相关，可以返回空数组。\n"
                + "只返回匹配度 >= 50 的结果，并按分数从高到低排序，最多返回前 " + RESULT_LIMIT + " 个。\n"
                + "请严格返回 JSON，格式为：\n"
                + "{\"matches\":[{\"talentId\":\"1\",\"score\":86,\"reason\":\"不超过40字的匹配理由\"}]}\n"
                + "不要输出任何额外说明。";

        StringBuilder prompt = new StringBuilder();
        prompt.append("岗位自由输入：").append(safe(freeTextQuery)).append("\n");
        prompt.append("岗位补充信息：").append(safe(jobSummary)).append("\n\n");
        prompt.append("候选人才（已由系统做过首轮召回，请继续做语义精排）：\n");
        appendCandidates(prompt, shortlist, "talentId", item ->
                "- talentId: " + item.id()
                + "\n  姓名: " + safe(item.payload().get("realName"))
                + "\n  用户名: " + safe(item.payload().get("username"))
                + "\n  性别: " + safe(item.payload().get("gender"))
                + "\n  技能: " + safe(item.payload().get("skills"))
                + "\n  学历: " + safe(item.payload().get("education"))
                + "\n  毕业院校: " + safe(item.payload().get("graduationSchool"))
                + "\n  专业: " + safe(item.payload().get("major"))
                + "\n  工作年限: " + safe(item.payload().get("workYears"))
                + "\n  期望岗位: " + safe(item.payload().get("expectedPosition"))
                + "\n  期望薪资: " + safe(item.payload().get("expectedSalary"))
                + "\n  经验: " + safe(item.payload().get("experience"))
                + "\n  项目经历: " + safe(item.payload().get("projectExperience"))
                + "\n  个人简介: " + safe(item.payload().get("selfIntroduction"))
                + "\n  证书成果: " + safe(item.payload().get("certificates"))
                + "\n  城市: " + safe(item.payload().get("city"))
                + "\n  系统召回提示: " + safe(item.reason()) + "\n"
        );

        return rerankWithAi(systemMessage, prompt.toString(), shortlist, "talentId");
    }

    // ==================== 内部方法 ====================

    private void appendCandidates(StringBuilder builder,
                                  List<CandidateScore> candidates,
                                  String idKey,
                                  java.util.function.Function<CandidateScore, String> formatter) {
        for (CandidateScore candidate : candidates) {
            if (candidate.payload().get(idKey) == null) {
                continue;
            }
            builder.append(formatter.apply(candidate));
        }
    }

    private Map<String, Object> rerankWithAi(String systemMessage,
                                             String prompt,
                                             List<CandidateScore> shortlist,
                                             String idKey) {
        try {
            String response = callDeepSeek(systemMessage, prompt);
            String jsonStr = extractJsonFromResponse(response);
            if (jsonStr != null && !jsonStr.isBlank()) {
                Map<String, Object> aiResult = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
                if (aiResult.get("matches") instanceof List<?>) {
                    List<Map<String, Object>> merged = mergeAiMatches(aiResult, shortlist, idKey);
                    Map<String, Object> result = new HashMap<>();
                    result.put("matches", merged);
                    return result;
                }
            }
        } catch (Exception e) {
            log.error("Failed to rerank candidates with AI", e);
        }

        Map<String, Object> fallback = new HashMap<>();
        fallback.put("matches", shortlist.stream()
                .limit(RESULT_LIMIT)
                .map(this::toFallbackMatch)
                .filter(item -> clampScore(item.get("score"), 0) >= MIN_FINAL_SCORE)
                .collect(Collectors.toList()));
        fallback.put("error", "AI service unavailable or response parsing failed");
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mergeAiMatches(Map<String, Object> aiResult,
                                                     List<CandidateScore> shortlist,
                                                     String idKey) {
        Object matchesValue = aiResult.get("matches");
        if (!(matchesValue instanceof List<?> rawMatches)) {
            return List.of();
        }

        Map<String, Map<String, Object>> aiMatchesById = new LinkedHashMap<>();
        for (Object rawMatch : rawMatches) {
            if (!(rawMatch instanceof Map<?, ?> rawMap)) {
                continue;
            }
            Map<String, Object> parsed = new LinkedHashMap<>();
            rawMap.forEach((key, value) -> parsed.put(String.valueOf(key), value));
            String id = safe(parsed.get(idKey));
            if (!id.isBlank()) {
                aiMatchesById.put(id, parsed);
            }
        }

        if (aiMatchesById.isEmpty()) {
            return List.of();
        }

        return shortlist.stream()
                .filter(candidate -> aiMatchesById.containsKey(candidate.id()))
                .map(candidate -> {
                    Map<String, Object> merged = new LinkedHashMap<>();
                    merged.putAll(candidate.payload());
                    Map<String, Object> aiMatch = aiMatchesById.get(candidate.id());
                    int heuristicScore = clampScore(candidate.score(), 0);
                    int aiScore = clampScore(aiMatch != null ? aiMatch.get("score") : null, heuristicScore);
                    int score = blendScore(heuristicScore, aiScore, aiMatch != null);
                    String reason = safe(aiMatch != null ? aiMatch.get("reason") : "");
                    if (reason.isBlank() || aiScore <= 5 || looksLikeMismatchReason(reason)) {
                        reason = fallbackReasonForUser(score);
                    }
                    merged.put("score", score);
                    merged.put("reason", reason);
                    return merged;
                })
                .filter(item -> clampScore(item.get("score"), 0) >= MIN_FINAL_SCORE)
                .sorted(Comparator.comparingInt(item -> -clampScore(item.get("score"), 0)))
                .limit(RESULT_LIMIT)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toFallbackMatch(CandidateScore candidate) {
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.putAll(candidate.payload());
        fallback.put("score", candidate.score());
        fallback.put("reason", fallbackReasonForUser(candidate.score()));
        return fallback;
    }

    private List<CandidateScore> shortlistCandidates(String queryText,
                                                     List<Map<String, String>> candidates,
                                                     String idKey,
                                                     java.util.function.Function<Map<String, String>, String> searchTextBuilder) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        List<String> queryTokens = tokenize(queryText);
        String normalizedQuery = normalize(queryText);
        int totalWeight = queryTokens.stream().mapToInt(this::tokenWeight).sum();

        List<CandidateScore> scored = new ArrayList<>();
        for (Map<String, String> candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            String id = safe(candidate.get(idKey));
            if (id.isBlank()) {
                continue;
            }

            String searchText = normalize(searchTextBuilder.apply(candidate));
            int rawScore = 0;
            List<String> matchedTokens = new ArrayList<>();

            if (!normalizedQuery.isBlank() && searchText.contains(normalizedQuery)) {
                rawScore += 28;
            }

            for (String token : queryTokens) {
                if (token.isBlank()) {
                    continue;
                }
                if (searchText.contains(token)) {
                    rawScore += tokenWeight(token);
                    if (matchedTokens.size() < 4 && !matchedTokens.contains(token)) {
                        matchedTokens.add(token);
                    }
                }
            }

            rawScore += looseCharacterOverlap(normalizedQuery, searchText);
            int score = normalizeHeuristicScore(rawScore, totalWeight, matchedTokens.size(), !normalizedQuery.isBlank());

            Map<String, String> payload = new LinkedHashMap<>(candidate);
            scored.add(new CandidateScore(id, score, buildHeuristicReason(matchedTokens, normalizedQuery, score), payload));
        }

        return scored.stream()
                .filter(item -> item.score() >= MIN_SHORTLIST_SCORE)
                .sorted(Comparator.comparingInt(CandidateScore::score).reversed())
                .limit(SHORTLIST_LIMIT)
                .collect(Collectors.toList());
    }

    private List<CandidateScore> buildAiCandidatePool(List<Map<String, String>> candidates,
                                                      String idKey,
                                                      java.util.function.Function<Map<String, String>, String> searchTextBuilder) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        return candidates.stream()
                .filter(Objects::nonNull)
                .map(candidate -> {
                    String id = safe(candidate.get(idKey));
                    String text = safe(searchTextBuilder.apply(candidate));
                    int richness = Math.min(text.length() / 10, 20);
                    return new CandidateScore(
                            id,
                            60 + richness,
                            "AI 候选项",
                            new LinkedHashMap<>(candidate)
                    );
                })
                .filter(item -> !item.id().isBlank())
                .sorted(Comparator.comparingInt(CandidateScore::score).reversed())
                .limit(SHORTLIST_LIMIT)
                .collect(Collectors.toList());
    }

    private String buildJobSearchText(Map<String, String> job) {
        return mergeText(
                job.get("title"),
                job.get("companyName"),
                job.get("industry"),
                job.get("scale"),
                job.get("companyDesc"),
                job.get("city"),
                job.get("salaryRange"),
                job.get("requirements"),
                job.get("description")
        );
    }

    private String buildTalentSearchText(Map<String, String> talent) {
        return mergeText(
                talent.get("realName"),
                talent.get("username"),
                talent.get("skills"),
                talent.get("education"),
                talent.get("gender"),
                talent.get("graduationSchool"),
                talent.get("major"),
                talent.get("workYears"),
                talent.get("expectedPosition"),
                talent.get("expectedSalary"),
                talent.get("experience"),
                talent.get("projectExperience"),
                talent.get("selfIntroduction"),
                talent.get("certificates"),
                talent.get("city")
        );
    }

    private String buildHeuristicReason(List<String> matchedTokens, String queryText, int score) {
        if (!matchedTokens.isEmpty()) {
            return "已命中：" + String.join("、", matchedTokens) + "，与输入意图较接近";
        }
        if (score >= MIN_FINAL_SCORE) {
            return "与输入描述存在一定语义相关性";
        }
        if (!safe(queryText).isBlank()) {
            return "与输入相关度较弱";
        }
        return "根据补充信息完成综合推荐";
    }

    private int looseCharacterOverlap(String queryText, String searchText) {
        if (queryText == null || queryText.isBlank() || searchText == null || searchText.isBlank()) {
            return 0;
        }
        LinkedHashSet<String> chars = new LinkedHashSet<>();
        for (int i = 0; i < queryText.length(); i++) {
            char current = queryText.charAt(i);
            if (Character.isWhitespace(current)) {
                continue;
            }
            String token = String.valueOf(current);
            if (searchText.contains(token)) {
                chars.add(token);
            }
        }
        return Math.min(chars.size(), 8);
    }

    private List<String> tokenize(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return List.of();
        }

        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        Matcher matcher = TOKEN_PATTERN.matcher(normalized);
        while (matcher.find()) {
            String token = matcher.group().trim();
            if (token.length() < 2) {
                continue;
            }
            tokens.add(token);
            if (containsChinese(token) && token.length() <= 8) {
                for (int i = 0; i < token.length() - 1; i++) {
                    tokens.add(token.substring(i, i + 2));
                }
            }
        }
        return new ArrayList<>(tokens);
    }

    private boolean containsChinese(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.UnicodeScript.of(text.charAt(i)) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    private int tokenWeight(String token) {
        if (token == null) {
            return 0;
        }
        if (containsChinese(token)) {
            return Math.min(6 + token.length() * 2, 18);
        }
        return Math.min(4 + token.length(), 14);
    }

    private int clampScore(Object rawScore, int fallback) {
        if (rawScore == null) {
            return Math.max(0, Math.min(100, fallback));
        }
        try {
            int value = Integer.parseInt(String.valueOf(rawScore));
            return Math.max(0, Math.min(100, value));
        } catch (Exception ignored) {
            return Math.max(0, Math.min(100, fallback));
        }
    }

    private int normalizeHeuristicScore(int rawScore, int totalWeight, int matchedCount, boolean hasQuery) {
        if (!hasQuery) {
            return 0;
        }
        int denominator = Math.max(totalWeight, 20);
        int normalized = (int) Math.round((rawScore * 100.0) / denominator);
        normalized += Math.min(matchedCount * 6, 18);
        return Math.max(0, Math.min(100, normalized));
    }

    /**
     * AI 分数权重 75%，启发式仅 25%——语义理解优先于关键词匹配。
     */
    private int blendScore(int heuristicScore, int aiScore, boolean hasAiMatch) {
        if (!hasAiMatch || aiScore <= 5) {
            return heuristicScore;
        }
        int blended = (int) Math.round(heuristicScore * 0.25 + aiScore * 0.75);
        return Math.max(0, Math.min(100, blended));
    }

    private boolean looksLikeMismatchReason(String reason) {
        String text = safe(reason);
        return text.contains("为空")
                || text.contains("无法评估")
                || text.contains("无法匹配")
                || text.contains("打 0 分")
                || text.contains("0 分")
                || text.contains("候选池");
    }

    private String fallbackReasonForUser(int score) {
        if (score >= 80) {
            return "AI 综合判断与输入意图高度相关";
        }
        if (score >= 65) {
            return "AI 综合判断与输入意图较为匹配";
        }
        return "AI 综合判断与输入意图存在一定相关性";
    }

    private String mergeText(String... texts) {
        return java.util.Arrays.stream(texts)
                .filter(Objects::nonNull)
                .map(this::safe)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("；"));
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String normalize(String text) {
        return safe(text)
                .toLowerCase(Locale.ROOT)
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('，', ' ')
                .replace('。', ' ')
                .replace('；', ' ')
                .replace('：', ' ')
                .replace(',', ' ')
                .replace(';', ' ')
                .replace(':', ' ')
                .replace('/', ' ')
                .replace('\\', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Map<String, Object> buildEmptyResult(String error) {
        Map<String, Object> result = new HashMap<>();
        result.put("matches", List.of());
        result.put("error", error);
        return result;
    }

    private record CandidateScore(String id, int score, String reason, Map<String, String> payload) {}

    // ==================== 人才展示推荐 ====================

    public Map<String, Object> recommendShowcaseTalents(List<Map<String, String>> talents, int limit) {
        String systemMessage = "你是一位资深人力资源专家。请综合考虑人才档案的所有信息（技能广度与深度、学历与毕业院校、专业匹配度、\n"
                + "工作经历与项目经历的含金量、证书成果、个人简介的表达质量、档案完整度等一切你能获取的维度），\n"
                + "评估哪些人才最适合作为「优秀人才展示」推荐给企业。请严格返回JSON格式。";
        StringBuilder userPromptBuilder = new StringBuilder();
        userPromptBuilder.append("人才列表（共").append(talents != null ? talents.size() : 0).append("人）：\n");
        if (talents != null && !talents.isEmpty()) {
            for (Map<String, String> t : talents) {
                userPromptBuilder.append("- talentId: ").append(t.getOrDefault("talentId", t.getOrDefault("id", "")));
                userPromptBuilder.append(", 姓名: ").append(t.getOrDefault("realName", ""));
                userPromptBuilder.append(", 性别: ").append(t.getOrDefault("gender", ""));
                userPromptBuilder.append(", 技能: ").append(t.getOrDefault("skills", ""));
                userPromptBuilder.append(", 学历: ").append(t.getOrDefault("education", ""));
                userPromptBuilder.append(", 毕业院校: ").append(t.getOrDefault("graduationSchool", ""));
                userPromptBuilder.append(", 专业: ").append(t.getOrDefault("major", ""));
                userPromptBuilder.append(", 工作年限: ").append(t.getOrDefault("workYears", ""));
                userPromptBuilder.append(", 期望岗位: ").append(t.getOrDefault("expectedPosition", ""));
                userPromptBuilder.append(", 期望薪资: ").append(t.getOrDefault("expectedSalary", ""));
                userPromptBuilder.append(", 经历: ").append(t.getOrDefault("experience", ""));
                userPromptBuilder.append(", 项目经历: ").append(t.getOrDefault("projectExperience", ""));
                userPromptBuilder.append(", 简介: ").append(t.getOrDefault("selfIntroduction", ""));
                userPromptBuilder.append(", 证书成果: ").append(t.getOrDefault("certificates", ""));
                userPromptBuilder.append(", 城市: ").append(t.getOrDefault("city", ""));
                userPromptBuilder.append("\n");
            }
        }
        userPromptBuilder.append("\n请从中选出最适合展示的 top ").append(limit).append(" 位，按推荐度排序。");
        userPromptBuilder.append("\n返回JSON格式：{\"recommendations\": [{\"talentId\": \"1\", \"score\": 90, \"reason\": \"技能匹配、档案完整\"}, ...]}");

        try {
            String response = callDeepSeek(systemMessage, userPromptBuilder.toString());
            String jsonStr = extractJsonFromResponse(response);
            if (jsonStr != null && !jsonStr.isBlank()) {
                return objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.error("Failed to parse recommendShowcaseTalents response", e);
        }

        Map<String, Object> fallback = new HashMap<>();
        fallback.put("recommendations", List.of());
        fallback.put("error", "AI service unavailable or response parsing failed");
        return fallback;
    }

    // ==================== 课程推荐 ====================

    public Map<String, Object> recommendCourses(String learnerProfile, List<String> completedCourses, List<Map<String, String>> availableCourses) {
        String systemMessage = "你是一位专业的学习顾问。请综合用户的技能背景、学历、职业目标、当前能力水平、已完成课程，\n"
                + "以及可选课程的全部信息（名称、描述、分类、讲师、时长、章节数等所有维度），\n"
                + "对所有可选课程进行推荐评估。不要只推荐少数几个——只要课程对用户有学习价值，都应该纳入推荐列表。\n"
                + "通过 priority 数字体现优先级（1 为最推荐，数字越大优先级越低），让用户自己决定学哪些。\n"
                + "推荐理由应结合用户实际情况说明为什么推荐这门课（技能补充、职业发展、学习路径衔接等）。\n"
                + "已完成的课程不要重复推荐。考虑学习路径的合理性和连贯性。\n"
                + "请严格返回JSON格式。";
        StringBuilder userPromptBuilder = new StringBuilder();
        userPromptBuilder.append("用户档案：").append(learnerProfile != null ? learnerProfile : "").append("\n");
        userPromptBuilder.append("已完成课程：").append(completedCourses != null ? completedCourses : List.of()).append("\n\n");
        userPromptBuilder.append("可选课程：\n");
        if (availableCourses != null && !availableCourses.isEmpty()) {
            for (Map<String, String> course : availableCourses) {
                userPromptBuilder.append("- courseId: ").append(course.getOrDefault("courseId", course.getOrDefault("id", "")));
                userPromptBuilder.append(", 名称: ").append(course.getOrDefault("title", course.getOrDefault("name", "")));
                userPromptBuilder.append(", 描述: ").append(course.getOrDefault("description", ""));
                userPromptBuilder.append(", 分类: ").append(course.getOrDefault("category", ""));
                userPromptBuilder.append(", 讲师: ").append(course.getOrDefault("teacher", ""));
                userPromptBuilder.append(", 时长(秒): ").append(course.getOrDefault("duration", ""));
                userPromptBuilder.append(", 章节数: ").append(course.getOrDefault("chapterCount", ""));
                userPromptBuilder.append("\n");
            }
        }
        userPromptBuilder.append("\n请返回JSON格式：{\"recommendations\": [{\"courseId\": \"1\", \"reason\": \"xxx\", \"priority\": 1}, ...]}");

        try {
            String response = callDeepSeek(systemMessage, userPromptBuilder.toString());
            String jsonStr = extractJsonFromResponse(response);
            if (jsonStr != null && !jsonStr.isBlank()) {
                return objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.error("Failed to parse recommendCourses response", e);
        }

        Map<String, Object> fallback = new HashMap<>();
        fallback.put("recommendations", List.of());
        fallback.put("error", "AI service unavailable or response parsing failed");
        return fallback;
    }

    // ==================== 职业评估 ====================

    public Map<String, Object> assessCareer(String name,
                                            String skills,
                                            String education,
                                            String experience,
                                            String major,
                                            String workYears,
                                            String expectedPosition,
                                            String projectExperience,
                                            String selfIntroduction,
                                            String certificates,
                                            String gender,
                                            String graduationSchool,
                                            String expectedSalary,
                                            String city) {
        String systemMessage = "你是一位资深的职业能力评估专家。请综合该人才的所有信息（技能、学历、毕业院校、专业、工作经历、项目经历、\n"
                + "证书成果、个人简介、期望岗位、所在城市等一切可用信息）进行全面的职业能力评估。\n"
                + "请严格返回JSON格式，包含以下字段：\n"
                + "radarData（雷达图数据，包含6个维度：技术能力、项目经验、学习能力、沟通协作、创新思维、行业知识，每个维度0-100分）、\n"
                + "overallScore（综合评分0-100）、report（详细评估报告，300字以内）、suggestions（发展建议，3-5条，数组形式）。";
        StringBuilder userPromptBuilder = new StringBuilder();
        userPromptBuilder.append("姓名：").append(name != null ? name : "").append("\n");
        userPromptBuilder.append("性别：").append(gender != null ? gender : "").append("\n");
        userPromptBuilder.append("技能：").append(skills != null ? skills : "").append("\n");
        userPromptBuilder.append("学历：").append(education != null ? education : "").append("\n");
        userPromptBuilder.append("毕业院校：").append(graduationSchool != null ? graduationSchool : "").append("\n");
        userPromptBuilder.append("专业：").append(major != null ? major : "").append("\n");
        userPromptBuilder.append("工作年限：").append(workYears != null ? workYears : "").append("\n");
        userPromptBuilder.append("期望岗位：").append(expectedPosition != null ? expectedPosition : "").append("\n");
        userPromptBuilder.append("期望薪资：").append(expectedSalary != null ? expectedSalary : "").append("\n");
        userPromptBuilder.append("所在城市：").append(city != null ? city : "").append("\n");
        userPromptBuilder.append("经历：").append(experience != null ? experience : "").append("\n");
        userPromptBuilder.append("项目经历：").append(projectExperience != null ? projectExperience : "").append("\n");
        userPromptBuilder.append("个人简介：").append(selfIntroduction != null ? selfIntroduction : "").append("\n");
        userPromptBuilder.append("证书成果：").append(certificates != null ? certificates : "").append("\n");

        try {
            String response = callDeepSeek(systemMessage, userPromptBuilder.toString());
            String jsonStr = extractJsonFromResponse(response);
            if (jsonStr != null && !jsonStr.isBlank()) {
                return objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.error("Failed to parse assessCareer response", e);
        }

        Map<String, Object> fallback = new HashMap<>();
        fallback.put("radarData", Map.of("技术能力", 0, "项目经验", 0, "学习能力", 0, "沟通协作", 0, "创新思维", 0, "行业知识", 0));
        fallback.put("overallScore", 0);
        fallback.put("report", "");
        fallback.put("suggestions", List.of());
        fallback.put("error", "AI service unavailable or response parsing failed");
        return fallback;
    }
}
