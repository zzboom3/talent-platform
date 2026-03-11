package com.talent.platform.controller;

import com.talent.platform.common.Result;
import com.talent.platform.entity.News;
import com.talent.platform.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {
    private static final List<String> PUBLIC_HIDDEN_SOURCE_SITES = List.of("开源中国");

    private final NewsRepository newsRepo;

    @GetMapping
    public Result<List<News>> list(@RequestParam(required = false) String category) {
        if (category != null) {
            try {
                News.Category cat = News.Category.valueOf(category.toUpperCase());
                return Result.ok(newsRepo.findPublicPublishedByCategoryExcludingSourcesOrderByPublishTimeDesc(
                        cat,
                        News.ReviewStatus.APPROVED,
                        PUBLIC_HIDDEN_SOURCE_SITES
                ));
            } catch (Exception ignored) {}
        }
        return Result.ok(newsRepo.findPublicPublishedExcludingSourcesOrderByPublishTimeDesc(
                News.ReviewStatus.APPROVED,
                PUBLIC_HIDDEN_SOURCE_SITES
        ));
    }

    @GetMapping("/{id}")
    public Result<News> getById(@PathVariable Long id) {
        return newsRepo.findById(id)
                .filter(news -> news.getReviewStatus() == null || news.getReviewStatus() == News.ReviewStatus.APPROVED)
                .map(Result::ok)
                .orElse(Result.fail("不存在"));
    }

    @PostMapping
    public Result<News> create(@RequestBody News news) {
        if (news.getCategory() != News.Category.ANNOUNCE) {
            return Result.fail("资讯和政策法规需通过采集后审核发布，公告请手动录入");
        }
        news.setCategory(News.Category.ANNOUNCE);
        news.setSourceType(News.SourceType.MANUAL);
        news.setReviewStatus(News.ReviewStatus.APPROVED);
        if (news.getAuthor() == null || news.getAuthor().isBlank()) {
            news.setAuthor("管理员");
        }
        news.setSourceSite("后台录入");
        news.setReviewedAt(LocalDateTime.now());
        news.setReviewedBy("admin");
        return Result.ok(newsRepo.save(news));
    }

    @PutMapping("/{id}")
    public Result<News> update(@PathVariable Long id, @RequestBody News updated) {
        News news = newsRepo.findById(id).orElse(null);
        if (news == null) return Result.fail("不存在");
        if (news.getCategory() != News.Category.ANNOUNCE || news.getSourceType() != News.SourceType.MANUAL) {
            return Result.fail("只有手工公告支持直接编辑");
        }
        news.setTitle(updated.getTitle());
        news.setContent(updated.getContent());
        news.setCategory(News.Category.ANNOUNCE);
        news.setAuthor(updated.getAuthor());
        news.setSourceSite("后台录入");
        news.setReviewStatus(News.ReviewStatus.APPROVED);
        return Result.ok(newsRepo.save(news));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        newsRepo.deleteById(id);
        return Result.ok();
    }
}
