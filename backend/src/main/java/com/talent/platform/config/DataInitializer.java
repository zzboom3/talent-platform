package com.talent.platform.config;

import com.talent.platform.entity.*;
import com.talent.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final NewsRepository newsRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initAdmin();
        if (companyRepository.count() == 0) {
            seedDemoData();
        }
    }

    private void initAdmin() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@talent-platform.com");
            admin.setRole(User.Role.ADMIN);
            userRepository.save(admin);
            log.info(">>> 已创建默认管理员账号 admin / admin123");
        }
    }

    private void seedDemoData() {
        log.info(">>> 开始初始化演示数据...");

        // --- 企业用户 + 企业信息 ---
        Company c1 = createCompany("jingzhou_tech", "京州智科技术有限公司", "信息技术",
                "专注于企业级软件开发和数字化转型解决方案，服务京州市200余家企事业单位。",
                "hr@jingzhou-tech.com", "028-88001001", "100-499人", "京州市高新区天府大道388号");
        Company c2 = createCompany("xingyun_data", "星云数据科技有限公司", "大数据/AI",
                "致力于大数据分析与人工智能应用，为政企客户提供智能决策支持平台。",
                "recruit@xingyun-data.com", "028-88002002", "50-99人", "京州市武侯区科华北路66号");
        Company c3 = createCompany("lanyue_net", "蓝月网络科技有限公司", "互联网",
                "移动互联网产品研发公司，旗下拥有多款日活百万级应用产品。",
                "jobs@lanyue.net", "028-88003003", "500-999人", "京州市锦江区东大街100号");

        // --- 职位 ---
        createJob(c1, "Java高级开发工程师", "负责核心业务系统的架构设计与开发，参与技术方案评审。",
                "5年以上Java开发经验；精通Spring Boot/Cloud微服务架构；熟悉MySQL、Redis、消息队列。",
                "20K-35K", "京州市");
        createJob(c1, "前端开发工程师", "负责公司Web产品的前端开发与性能优化。",
                "3年以上前端开发经验；精通Vue3/React；熟悉TypeScript和前端工程化。",
                "15K-25K", "京州市");
        createJob(c2, "数据分析师", "负责用户行为数据的建模分析，输出业务洞察报告。",
                "统计学或计算机相关专业；熟练使用Python、SQL；有BI工具使用经验。",
                "12K-20K", "京州市");
        createJob(c2, "AI算法工程师", "负责NLP/CV算法的研发与模型优化落地。",
                "硕士及以上学历；熟悉PyTorch/TensorFlow；有大模型微调经验优先。",
                "25K-45K", "京州市");
        createJob(c3, "产品经理", "负责移动端产品规划、需求分析和项目推进。",
                "3年以上互联网产品经验；优秀的逻辑分析和沟通能力；有C端产品经验优先。",
                "18K-30K", "京州市");
        createJob(c3, "Flutter开发工程师", "负责公司核心App的跨平台开发与维护。",
                "2年以上Flutter开发经验；了解iOS/Android原生开发；有上线App作品优先。",
                "15K-28K", "京州市");

        // --- 新闻/公告/政策 ---
        createNews("京州市发布2026年软件产业人才引进计划",
                "<p>京州市人力资源和社会保障局近日发布《2026年软件产业人才引进实施办法》，计划引进高层次软件人才500名。符合条件的人才将享受安家补贴、子女入学等优惠政策。</p><p>该计划重点支持人工智能、大数据、云计算、网络安全四大方向，对入选者提供最高50万元的安家补贴和3年期的免费人才公寓。</p>",
                News.Category.POLICY, "京州市人社局");
        createNews("第五届京州软件人才招聘大会将于4月举办",
                "<p>由京州市软件行业协会主办的第五届软件人才招聘大会定于2026年4月15日在京州国际会展中心举行。</p><p>本届大会预计有超过200家企业参展，提供岗位5000余个，涵盖开发、测试、产品、设计等多个方向。现场将设置简历诊断、职业规划咨询等配套服务。</p>",
                News.Category.NEWS, "京州软件协会");
        createNews("关于开展软件行业职业技能等级认定的通知",
                "<p>为推进软件产业人才队伍建设，经市人社局批准，现面向全市软件从业人员开展职业技能等级认定工作。</p><p>认定范围包括：软件开发、系统运维、数据分析、网络安全等方向，分为初级、中级、高级三个等级。通过认定者将获得全国通用的职业技能等级证书。</p>",
                News.Category.ANNOUNCE, "京州市人社局");
        createNews("京州高新区出台软件企业税收优惠新政",
                "<p>京州高新区管委会日前出台《关于促进软件产业高质量发展的若干措施》，对符合条件的软件企业给予企业所得税减免、研发费用加计扣除等优惠。</p><p>新政还提出设立总规模10亿元的软件产业发展基金，重点投资种子期和成长期的软件企业。</p>",
                News.Category.POLICY, "京州高新区管委会");

        // --- 课程 ---
        createCourse("Java微服务架构实战", "从单体到微服务，系统讲解Spring Cloud Alibaba技术栈在实际项目中的应用。",
                "张教授", "后端开发", 2400, 24);
        createCourse("Vue3全家桶企业实战", "基于Vue3 + TypeScript + Pinia + Element Plus构建企业级管理系统。",
                "李老师", "前端开发", 1800, 18);
        createCourse("Python数据分析与可视化", "使用Pandas、NumPy、Matplotlib等工具进行数据清洗、分析和可视化展示。",
                "王博士", "数据分析", 1200, 12);
        createCourse("大模型应用开发入门", "了解大语言模型原理，学习使用API进行智能应用开发，包括RAG和Agent实战。",
                "陈教授", "人工智能", 1600, 16);
        createCourse("软件测试从入门到精通", "覆盖功能测试、接口测试、自动化测试、性能测试全流程实战。",
                "刘老师", "软件测试", 900, 10);

        log.info(">>> 演示数据初始化完成");
    }

    private Company createCompany(String username, String companyName, String industry,
                                   String description, String email, String phone,
                                   String scale, String address) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("demo123"));
        user.setEmail(email);
        user.setRole(User.Role.ENTERPRISE);
        userRepository.save(user);

        Company company = new Company();
        company.setUser(user);
        company.setCompanyName(companyName);
        company.setIndustry(industry);
        company.setDescription(description);
        company.setContactEmail(email);
        company.setContactPhone(phone);
        company.setScale(scale);
        company.setAddress(address);
        company.setAuditStatus(Company.AuditStatus.APPROVED);
        return companyRepository.save(company);
    }

    private void createJob(Company company, String title, String description,
                           String requirements, String salaryRange, String city) {
        Job job = new Job();
        job.setCompany(company);
        job.setTitle(title);
        job.setDescription(description);
        job.setRequirements(requirements);
        job.setSalaryRange(salaryRange);
        job.setCity(city);
        jobRepository.save(job);
    }

    private void createNews(String title, String content, News.Category category, String author) {
        News news = new News();
        news.setTitle(title);
        news.setContent(content);
        news.setCategory(category);
        news.setAuthor(author);
        news.setSourceType(News.SourceType.MANUAL);
        news.setReviewStatus(News.ReviewStatus.APPROVED);
        newsRepository.save(news);
    }

    private void createCourse(String title, String description, String teacher,
                              String category, int duration, int chapterCount) {
        Course course = new Course();
        course.setTitle(title);
        course.setDescription(description);
        course.setTeacher(teacher);
        course.setCategory(category);
        course.setDuration(duration);
        course.setChapterCount(chapterCount);
        courseRepository.save(course);
    }
}
