package com.talent.platform.controller;

import com.talent.platform.common.Result;
import com.talent.platform.entity.News;
import com.talent.platform.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/news")
@RequiredArgsConstructor
public class AdminNewsController {

    private final NewsRepository newsRepository;

    @GetMapping
    public Result<List<News>> list(@RequestParam(required = false) String category,
                                   @RequestParam(required = false) String reviewStatus,
                                   @RequestParam(required = false) String sourceType,
                                   @RequestParam(required = false) String keyword) {
        News.Category categoryEnum = parseEnum(category, News.Category.class);
        News.ReviewStatus reviewStatusEnum = parseEnum(reviewStatus, News.ReviewStatus.class);
        News.SourceType sourceTypeEnum = parseEnum(sourceType, News.SourceType.class);
        String normalizedKeyword = normalizeKeyword(keyword);

        return Result.ok(newsRepository.searchAdminNews(
                categoryEnum,
                reviewStatusEnum,
                sourceTypeEnum,
                normalizedKeyword
        ));
    }

    @PostMapping("/announcements")
    public Result<News> createAnnouncement(@RequestBody News news,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        news.setId(null);
        news.setCategory(News.Category.ANNOUNCE);
        news.setSourceType(News.SourceType.MANUAL);
        news.setReviewStatus(News.ReviewStatus.APPROVED);
        news.setSourceSite("后台录入");
        news.setSourceUrl(null);
        news.setReviewedAt(LocalDateTime.now());
        news.setReviewedBy(userDetails.getUsername());
        return Result.ok(newsRepository.save(news));
    }

    @PutMapping("/announcements/{id}")
    public Result<News> updateAnnouncement(@PathVariable Long id,
                                           @RequestBody News updated,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        News news = newsRepository.findById(id).orElse(null);
        if (news == null) return Result.fail("资讯不存在");
        if (news.getCategory() != News.Category.ANNOUNCE || news.getSourceType() != News.SourceType.MANUAL) {
            return Result.fail("仅手工公告支持编辑");
        }
        news.setTitle(updated.getTitle());
        news.setContent(updated.getContent());
        news.setAuthor(updated.getAuthor());
        news.setCategory(News.Category.ANNOUNCE);
        news.setSourceType(News.SourceType.MANUAL);
        news.setReviewStatus(News.ReviewStatus.APPROVED);
        news.setSourceSite("后台录入");
        news.setReviewedAt(LocalDateTime.now());
        news.setReviewedBy(userDetails.getUsername());
        return Result.ok(newsRepository.save(news));
    }

    @PutMapping("/{id}/review")
    public Result<News> review(@PathVariable Long id,
                               @RequestBody News updated,
                               @AuthenticationPrincipal UserDetails userDetails) {
        News news = newsRepository.findById(id).orElse(null);
        if (news == null) return Result.fail("资讯不存在");
        if (news.getSourceType() != News.SourceType.CRAWLED) {
            return Result.fail("手工公告无需审核");
        }
        if (updated.getReviewStatus() == null) {
            return Result.fail("缺少审核状态");
        }

        news.setReviewStatus(updated.getReviewStatus());
        news.setReviewedAt(LocalDateTime.now());
        news.setReviewedBy(userDetails.getUsername());
        return Result.ok(newsRepository.save(news));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        newsRepository.deleteById(id);
        return Result.ok();
    }

    private <E extends Enum<E>> E parseEnum(String rawValue, Class<E> enumType) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
