package com.talent.platform.service;

import com.talent.platform.entity.Company;
import com.talent.platform.entity.CrawlerRunLog;
import com.talent.platform.entity.Job;
import com.talent.platform.entity.News;
import com.talent.platform.entity.User;
import com.talent.platform.repository.CompanyRepository;
import com.talent.platform.repository.CrawlerRunLogRepository;
import com.talent.platform.repository.JobRepository;
import com.talent.platform.repository.NewsRepository;
import com.talent.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerService {
    private static final SSLSocketFactory INSECURE_SSL_SOCKET_FACTORY = buildInsecureSslSocketFactory();

    private static final String CRAWLER_USERNAME = "crawler_bot";
    private static final String CRAWLER_COMPANY_NAME = "平台采集中心";
    private static final String JOB_SOURCE_URL = "https://remoteok.com/remote-dev-jobs";
    private static final String CSTP_NEWS_URL = "https://www.cstpchina.cn/";
    private static final String CRAWLED_JOB_DESCRIPTION_MARKER = "数据来源: Remote OK";
    private static final String CRAWLED_JOB_REQUIREMENTS_MARKER = "来源站点: Remote OK";
    private static final List<String> CRAWLED_NEWS_SITES = List.of("中国软件专业人才培养工程");
    private static final List<String> INDUSTRY_NEWS_TALENT_KEYWORDS = List.of(
            "人才", "就业", "招聘", "实习", "校招", "春招", "秋招", "毕业生", "高校毕业生", "大学生",
            "职业", "培训", "技能", "技能人才", "人力资源", "用工", "岗位", "人才培养", "产教融合", "职业教育"
    );
    private static final List<String> INDUSTRY_NEWS_INDUSTRY_KEYWORDS = List.of(
            "软件", "软件产业", "数字经济", "数字化", "人工智能", "大模型", "算力", "工业软件",
            "信息技术", "互联网", "互联网服务", "产业园", "科技企业", "数字人才", "工业互联网"
    );
    private static final List<String> INDUSTRY_NEWS_STRONG_PHRASES = List.of(
            "软件人才", "数字人才", "软件产业人才", "人工智能人才", "工业互联网", "人工智能应用"
    );
    private static final List<String> INDUSTRY_NEWS_EXCLUDE_KEYWORDS = List.of(
            "开源项目", "版本发布", "漏洞", "补丁", "内核", "框架", "编程语言", "数据库", "浏览器", "手机",
            "测评", "跑分", "新品发布", "消费电子", "汽车评测", "游戏", "动漫", "影视"
    );
    private static final List<String> POLICY_DOCUMENT_KEYWORDS = List.of(
            "政策", "办法", "条例", "通知", "意见", "规定", "实施", "细则", "措施", "方案", "公告", "指南"
    );
    private static final List<String> POLICY_THEME_KEYWORDS = List.of(
            "人才", "就业", "创业", "招聘", "高校毕业生", "毕业生", "职业", "培训", "技能",
            "软件", "数字经济", "产业", "人社", "人力资源", "企业", "创新", "公共服务"
    );

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final NewsRepository newsRepository;
    private final CrawlerRunLogRepository crawlerRunLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    private Map<String, Object> lastStatus = defaultStatus();
    private boolean running = false;

    public synchronized Map<String, Object> getStatus() {
        if (running) {
            Map<String, Object> result = new LinkedHashMap<>(defaultStatus());
            result.putAll(buildRealtimeTotals());
            result.putAll(lastStatus);
            result.put("running", true);
            return result;
        }

        Map<String, Object> result = new LinkedHashMap<>(defaultStatus());
        result.putAll(buildRealtimeTotals());

        try {
            crawlerRunLogRepository.findTop1ByOrderByFinishedAtDesc().ifPresentOrElse(
                    log -> {
                        result.put("success", log.isSuccess());
                        result.put("startedAt", log.getStartedAt());
                        result.put("finishedAt", log.getFinishedAt());
                        result.put("jobsInserted", log.getJobsInserted());
                        result.put("newsInserted", log.getNewsInserted());
                        result.put("totalInserted", log.getTotalInserted());
                        result.put("errorCount", log.getErrorCount());
                        result.put("messages", log.getMessages());
                        result.put("industryNewsInserted", log.getNewsInserted()); // 日志未区分细类时保持兼容
                        result.put("policyInserted", 0);
                    },
                    () -> result.putAll(lastStatus)
            );
        } catch (Exception e) {
            log.warn("load crawler run log failed, fallback to in-memory status", e);
            result.putAll(lastStatus);
        }
        result.put("running", false);
        return result;
    }

    public synchronized Map<String, Object> runCrawler() {
        return runCrawler(true);
    }

    public synchronized Map<String, Object> runNewsCrawler() {
        return runCrawler(false);
    }

    public synchronized Map<String, Object> startNewsCrawler() {
        if (running) {
            Map<String, Object> result = new LinkedHashMap<>(lastStatus);
            result.putAll(buildRealtimeTotals());
            result.put("running", true);
            result.putIfAbsent("messages", List.of("采集任务正在后台执行"));
            return result;
        }

        LocalDateTime startedAt = LocalDateTime.now();
        Map<String, Object> status = new LinkedHashMap<>(defaultStatus());
        status.put("success", false);
        status.put("running", true);
        status.put("startedAt", startedAt);
        status.put("finishedAt", null);
        status.put("messages", List.of("采集任务已启动，正在后台执行"));
        status.putAll(buildRealtimeTotals());
        lastStatus = status;
        running = true;

        Thread worker = new Thread(() -> {
            try {
                Map<String, Object> result = runCrawler(false);
                synchronized (CrawlerService.this) {
                    result.put("running", false);
                    lastStatus = result;
                    running = false;
                }
            } catch (Exception e) {
                log.warn("background crawler failed", e);
                synchronized (CrawlerService.this) {
                    Map<String, Object> failed = new LinkedHashMap<>(defaultStatus());
                    failed.put("success", false);
                    failed.put("running", false);
                    failed.put("startedAt", startedAt);
                    failed.put("finishedAt", LocalDateTime.now());
                    failed.put("errorCount", 1);
                    failed.put("messages", List.of("后台采集失败: " + e.getMessage()));
                    failed.putAll(buildRealtimeTotals());
                    lastStatus = failed;
                    running = false;
                }
            }
        }, "news-crawler-worker");
        worker.setDaemon(true);
        worker.start();

        return new LinkedHashMap<>(status);
    }

    private Map<String, Object> runCrawler(boolean includeJobs) {
        LocalDateTime startedAt = LocalDateTime.now();
        List<String> messages = new ArrayList<>();
        int jobsInserted = 0;
        int industryNewsInserted = 0;
        int policyInserted = 0;
        int newsInserted = 0;
        int errorCount = 0;

        if (includeJobs) {
            try {
                Company crawlerCompany = ensureCrawlerCompany();
                jobsInserted += crawlJobs(crawlerCompany, messages);
            } catch (Exception e) {
                errorCount++;
                messages.add("岗位抓取失败: " + e.getMessage());
                log.warn("crawl jobs failed", e);
            }
        } else {
            messages.add("本次仅执行资讯与政策采集，已跳过岗位抓取");
        }

        try {
            industryNewsInserted += crawlIndustryNews(messages);
        } catch (Exception e) {
            errorCount++;
            messages.add("行业资讯抓取失败: " + e.getMessage());
            log.warn("crawl industry news failed", e);
        }

        try {
            policyInserted += crawlPolicies(messages);
        } catch (Exception e) {
            errorCount++;
            messages.add("政策法规抓取失败: " + e.getMessage());
            log.warn("crawl policies failed", e);
        }

        newsInserted = industryNewsInserted + policyInserted;

        if (newsInserted > 0) {
            notificationService.notifyNewsPendingReview(newsInserted);
        }

        LocalDateTime finishedAt = LocalDateTime.now();
        int totalInserted = jobsInserted + newsInserted;
        boolean success = errorCount == 0 || totalInserted > 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("startedAt", startedAt);
        result.put("finishedAt", finishedAt);
        result.put("jobsInserted", jobsInserted);
        result.put("industryNewsInserted", industryNewsInserted);
        result.put("policyInserted", policyInserted);
        result.put("newsInserted", newsInserted);
        result.put("totalInserted", totalInserted);
        result.put("errorCount", errorCount);
        result.put("messages", messages);
        result.putAll(buildRealtimeTotals());
        result.put("running", false);

        // 持久化到数据库
        CrawlerRunLog log = new CrawlerRunLog();
        log.setStartedAt(startedAt);
        log.setFinishedAt(finishedAt);
        log.setJobsInserted(jobsInserted);
        log.setNewsInserted(newsInserted);
        log.setTotalInserted(totalInserted);
        log.setErrorCount(errorCount);
        log.setSuccess(success);
        log.setMessages(messages);
        crawlerRunLogRepository.save(log);

        lastStatus = result;
        return new LinkedHashMap<>(result);
    }

    private Company ensureCrawlerCompany() {
        User crawlerUser = userRepository.findByUsername(CRAWLER_USERNAME).orElseGet(() -> {
            User user = new User();
            user.setUsername(CRAWLER_USERNAME);
            user.setPassword(passwordEncoder.encode("crawler-bot-password"));
            user.setEmail("crawler@talent-platform.local");
            user.setRole(User.Role.ENTERPRISE);
            return userRepository.save(user);
        });

        return companyRepository.findByUser(crawlerUser).orElseGet(() -> {
            Company company = new Company();
            company.setUser(crawlerUser);
            company.setCompanyName("平台采集中心");
            company.setIndustry("互联网");
            company.setDescription("系统自动采集公开岗位数据使用，不对外展示联系方式。");
            company.setContactEmail("crawler@talent-platform.local");
            company.setAuditStatus(Company.AuditStatus.APPROVED);
            return companyRepository.save(company);
        });
    }

    private int crawlJobs(Company crawlerCompany, List<String> messages) throws IOException {
        Document doc = connect(JOB_SOURCE_URL);
        Elements rows = doc.select("tr.job");
        int inserted = 0;

        for (Element row : rows) {
            if (inserted >= 12) {
                break;
            }

            String title = textOrEmpty(row, "h2");
            if (title.isBlank() || jobRepository.existsByTitleAndCompanyId(title, crawlerCompany.getId())) {
                continue;
            }

            String companyName = textOrEmpty(row, "h3");
            String city = textOrEmpty(row, ".location");
            String salary = textOrEmpty(row, ".salary");
            String href = row.attr("data-href");
            String sourceUrl = href.startsWith("http") ? href : "https://remoteok.com" + href;

            Job job = new Job();
            job.setCompany(crawlerCompany);
            job.setTitle(title);
            job.setCity(city.isBlank() ? "远程" : city);
            job.setSalaryRange(salary.isBlank() ? "面议" : salary);
            job.setRequirements(CRAWLED_JOB_REQUIREMENTS_MARKER + "\n岗位公司: " + (companyName.isBlank() ? "未提供" : companyName));
            job.setDescription(buildJobDescription(title, companyName, sourceUrl));
            jobRepository.save(job);
            inserted++;
        }

        messages.add("岗位抓取完成，新增 " + inserted + " 条");
        return inserted;
    }

    private int crawlIndustryNews(List<String> messages) {
        int inserted = 0;
        inserted += crawlIndustryNewsPageSafely("中国软件专业人才培养工程", CSTP_NEWS_URL, "a[href*=View.aspx?id=]", messages, 8);
        messages.add("行业资讯抓取完成，新增 " + inserted + " 条待审核内容");
        return inserted;
    }

    private int crawlIndustryNewsPageSafely(String sourceSite, String url, String selector, List<String> messages, int limit) {
        try {
            return crawlIndustryNewsPage(sourceSite, url, selector, messages, limit);
        } catch (Exception e) {
            messages.add(sourceSite + " 抓取失败: " + e.getMessage());
            log.warn("crawl industry news source failed: {}", sourceSite, e);
            return 0;
        }
    }

    private int crawlIndustryNewsPage(String sourceSite, String url, String selector, List<String> messages, int limit) throws IOException {
        Document listDoc = connect(url);
        Elements links = listDoc.select(selector);
        int inserted = 0;
        int skippedOffTopic = 0;

        for (Element link : links) {
            if (inserted >= limit) {
                break;
            }

            String title = link.text().trim();
            String href = link.absUrl("href");
            if (!isIndustryNewsCandidate(title) || href.isBlank() || newsRepository.existsByTitle(title)) {
                continue;
            }

            String detailContent = fetchIndustryNewsDetail(sourceSite, href);
            if (!isIndustryNewsRelevant(sourceSite, title, "", detailContent)) {
                skippedOffTopic++;
                continue;
            }

            News news = buildCrawledNews(
                    title,
                    buildIndustryNewsContent(resolveIndustryNewsSummary("", detailContent), href),
                    News.Category.NEWS,
                    sourceSite,
                    href
            );
            news.setAuthor(sourceSite);
            newsRepository.save(news);
            inserted++;
        }

        if (skippedOffTopic > 0) {
            messages.add(sourceSite + " 过滤 " + skippedOffTopic + " 条非主题资讯");
        }
        return inserted;
    }

    private int crawlPolicies(List<String> messages) {
        messages.add("政策法规抓取完成，新增 0 条待审核内容（当前未配置政策源）");
        return 0;
    }

    private int crawlPolicyPageSafely(String sourceSite, String url, String selector, List<String> messages, int limit) {
        try {
            return crawlPolicyPage(sourceSite, url, selector, messages, limit);
        } catch (Exception e) {
            messages.add(sourceSite + " 政策抓取失败: " + e.getMessage());
            log.warn("crawl policy source failed: {}", sourceSite, e);
            return 0;
        }
    }

    private int crawlPolicyPage(String sourceSite, String url, String selector, List<String> messages, int limit) throws IOException {
        Document listDoc = connect(url);
        Elements links = listDoc.select(selector);
        int inserted = 0;
        int skippedOffTopic = 0;

        for (Element link : links) {
            if (inserted >= limit) {
                break;
            }

            String title = link.text().trim();
            String href = link.absUrl("href");
            if (!isPolicyCandidate(title) || href.isBlank() || newsRepository.existsByTitle(title)) {
                continue;
            }

            Document detailDoc = connect(href);
            String content = extractArticleText(detailDoc, sourceSite);
            if (content.isBlank()) {
                continue;
            }
            if (!isPolicyRelevant(title, content)) {
                skippedOffTopic++;
                continue;
            }

            News news = buildCrawledNews(title, content, News.Category.POLICY, sourceSite, href);
            news.setAuthor(sourceSite);
            newsRepository.save(news);
            inserted++;
        }
        if (skippedOffTopic > 0) {
            messages.add(sourceSite + " 过滤 " + skippedOffTopic + " 条非主题政策内容");
        }
        return inserted;
    }

    private Document connect(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .sslSocketFactory(INSECURE_SSL_SOCKET_FACTORY)
                .timeout(10000)
                .get();
    }

    private Document connectXml(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .sslSocketFactory(INSECURE_SSL_SOCKET_FACTORY)
                .timeout(10000)
                .parser(Parser.xmlParser())
                .get();
    }

    private String buildJobDescription(String title, String companyName, String sourceUrl) {
        StringBuilder builder = new StringBuilder();
        builder.append("岗位名称: ").append(title).append("\n");
        if (!companyName.isBlank()) {
            builder.append("企业名称: ").append(companyName).append("\n");
        }
        builder.append(CRAWLED_JOB_DESCRIPTION_MARKER).append("\n");
        builder.append("原始链接: ").append(sourceUrl).append("\n");
        builder.append("说明: 该岗位由平台自动抓取公开招聘信息生成，详情请以原始招聘页面为准。");
        return builder.toString();
    }

    private Map<String, Object> buildRealtimeTotals() {
        Map<String, Object> totals = new LinkedHashMap<>();
        long crawledJobsTotal = jobRepository.countCrawledJobs(CRAWLER_USERNAME, CRAWLED_JOB_DESCRIPTION_MARKER, CRAWLED_JOB_REQUIREMENTS_MARKER);
        long crawledNewsTotal = newsRepository.countCrawledNews(News.SourceType.CRAWLED.name(), CRAWLED_NEWS_SITES);
        long crawledPolicyCount = newsRepository.countCrawledNewsByCategory(News.Category.POLICY.name(), News.SourceType.CRAWLED.name(), CRAWLED_NEWS_SITES);
        long crawledIndustryNewsCount = newsRepository.countCrawledNewsByCategory(News.Category.NEWS.name(), News.SourceType.CRAWLED.name(), CRAWLED_NEWS_SITES);

        totals.put("crawledJobsTotal", crawledJobsTotal);
        totals.put("crawledNewsTotal", crawledNewsTotal);
        totals.put("crawledPolicyCount", crawledPolicyCount);
        totals.put("crawledIndustryNewsCount", crawledIndustryNewsCount);
        return totals;
    }

    private News buildCrawledNews(String title, String content, News.Category category, String sourceSite, String sourceUrl) {
        News news = new News();
        news.setTitle(title);
        news.setContent(content);
        news.setCategory(category);
        news.setSourceType(News.SourceType.CRAWLED);
        news.setReviewStatus(News.ReviewStatus.PENDING);
        news.setSourceSite(sourceSite);
        news.setSourceUrl(sourceUrl);
        return news;
    }

    private String extractArticleText(Document doc) {
        Elements paragraphs = doc.select(".pages_content p, .page-content p, .content p, p");
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (Element paragraph : paragraphs) {
            String text = paragraph.text().trim();
            if (text.length() < 20) {
                continue;
            }
            builder.append(text).append("\n\n");
            count++;
            if (count >= 12) {
                break;
            }
        }
        return builder.toString().trim();
    }

    private String extractArticleText(Document doc, String sourceSite) {
        String selector = resolveArticleSelector(sourceSite);
        if (!selector.isBlank()) {
            Element article = doc.selectFirst(selector);
            if (article != null) {
                String text = article.text().replace('\u00A0', ' ').trim();
                if (text.length() >= 40) {
                    return text.length() <= 1200 ? text : text.substring(0, 1200) + "...";
                }
            }
        }
        return extractArticleText(doc);
    }

    private String resolveArticleSelector(String sourceSite) {
        if ("中国软件行业协会教培分会".equals(sourceSite)) {
            return ".yj_text";
        }
        if ("中国软件专业人才培养工程".equals(sourceSite)) {
            return ".cont";
        }
        if ("中国就业网".equals(sourceSite)) {
            return ".detail_article";
        }
        if ("中国政府网".equals(sourceSite)) {
            return ".pages_content, .page-content, .content";
        }
        if ("山西省人社厅".equals(sourceSite)) {
            return ".TRS_Editor, .article, .content";
        }
        return "";
    }

    private String textOrEmpty(Element root, String selector) {
        Element element = root.selectFirst(selector);
        return element == null ? "" : element.text().trim();
    }

    private boolean isPolicyCandidate(String title) {
        if (title == null) {
            return false;
        }
        String value = title.trim();
        if (value.length() < 8) {
            return false;
        }
        return containsAny(value, POLICY_DOCUMENT_KEYWORDS) && containsAny(value, POLICY_THEME_KEYWORDS);
    }

    private boolean isIndustryNewsRelevant(String title, String description) {
        return isIndustryNewsRelevant("", title, description, "");
    }

    private boolean isIndustryNewsCandidate(String title) {
        String value = safeText(title);
        if (value.length() < 8) {
            return false;
        }
        if (containsAny(value, List.of("查看更多", "登录", "注册", "联系我们", "关于我们", "合作机构", "项目简介", "专家委员会"))) {
            return false;
        }
        return containsAny(value, INDUSTRY_NEWS_TALENT_KEYWORDS)
                || containsAny(value, INDUSTRY_NEWS_INDUSTRY_KEYWORDS)
                || containsAny(value, INDUSTRY_NEWS_STRONG_PHRASES);
    }

    private boolean isIndustryNewsRelevant(String sourceSite, String title, String description, String detailContent) {
        String titleText = safeText(title);
        String text = safeText(title + " " + description + " " + detailContent);
        if (text.isBlank() || containsAny(text, INDUSTRY_NEWS_EXCLUDE_KEYWORDS)) {
            return false;
        }

        int titleTalentHits = countMatches(titleText, INDUSTRY_NEWS_TALENT_KEYWORDS);
        int titleIndustryHits = countMatches(titleText, INDUSTRY_NEWS_INDUSTRY_KEYWORDS);
        int textTalentHits = countMatches(text, INDUSTRY_NEWS_TALENT_KEYWORDS);
        int textIndustryHits = countMatches(text, INDUSTRY_NEWS_INDUSTRY_KEYWORDS);
        boolean hasStrongPhrase = containsAny(titleText, INDUSTRY_NEWS_STRONG_PHRASES) || containsAny(text, INDUSTRY_NEWS_STRONG_PHRASES);
        boolean hasIndustry = titleIndustryHits > 0 || textIndustryHits > 0;
        boolean hasTalent = titleTalentHits > 0 || textTalentHits > 0;

        if (!hasIndustry) {
            return false;
        }
        if (titleIndustryHits == 0 && !containsAny(titleText, INDUSTRY_NEWS_STRONG_PHRASES)) {
            return false;
        }
        if ("中国就业网".equals(sourceSite) && !(titleIndustryHits > 0 && hasTalent)) {
            return false;
        }
        if (hasStrongPhrase && hasTalent) {
            return true;
        }

        boolean titleCrossDomain = titleTalentHits > 0 && titleIndustryHits > 0;
        boolean textCrossDomain = textTalentHits > 0 && textIndustryHits > 0;
        int score = titleTalentHits * 3 + titleIndustryHits * 3 + textTalentHits + textIndustryHits;

        return titleCrossDomain || (textCrossDomain && score >= 6);
    }

    private boolean isPolicyRelevant(String title, String content) {
        String text = (title + " " + content).trim();
        return containsAny(text, POLICY_THEME_KEYWORDS);
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return keywords.stream().anyMatch(text::contains);
    }

    private int countMatches(String text, List<String> keywords) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return (int) keywords.stream().filter(text::contains).count();
    }

    private String safeText(String text) {
        return text == null ? "" : text.replace('\u00A0', ' ').trim();
    }

    private String htmlToPlainText(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) {
            return "";
        }
        return Jsoup.parse(rawHtml).text().replace('\u00A0', ' ').trim();
    }

    private String buildIndustryNewsContent(String description, String link) {
        StringBuilder builder = new StringBuilder();
        if (!description.isBlank()) {
            builder.append(description).append("\n\n");
        } else {
            builder.append("该资讯摘要为空，请审核后查看原文。").append("\n\n");
        }
        builder.append("原文链接: ").append(link).append("\n");
        builder.append("说明: 系统仅保留与人才、就业、软件产业相关的资讯，详情请以原始页面为准。");
        return builder.toString();
    }

    private String fetchIndustryNewsDetail(String sourceSite, String link) {
        if (link == null || link.isBlank()) {
            return "";
        }
        try {
            return extractArticleText(connect(link), sourceSite);
        } catch (Exception e) {
            log.debug("load industry news detail failed: {}", link, e);
            return "";
        }
    }

    private String resolveIndustryNewsSummary(String description, String detailContent) {
        String preferred = detailContent == null || detailContent.isBlank() ? description : detailContent;
        if (preferred == null) {
            return "";
        }
        String compact = preferred.trim();
        if (compact.length() <= 600) {
            return compact;
        }
        return compact.substring(0, 600) + "...";
    }

    private static SSLSocketFactory buildInsecureSslSocketFactory() {
        try {
            TrustManager[] trustAllManagers = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllManagers, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new IllegalStateException("failed to init crawler ssl context", e);
        }
    }

    private Map<String, Object> defaultStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("startedAt", null);
        result.put("finishedAt", null);
        result.put("jobsInserted", 0);
        result.put("industryNewsInserted", 0);
        result.put("policyInserted", 0);
        result.put("newsInserted", 0);
        result.put("totalInserted", 0);
        result.put("errorCount", 0);
        result.put("messages", List.of("尚未执行数据采集"));
        result.put("running", false);
        return result;
    }
}
