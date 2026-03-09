package com.talent.platform.controller;

import com.talent.platform.common.Result;
import com.talent.platform.entity.News;
import com.talent.platform.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsRepository newsRepo;

    @GetMapping
    public Result<List<News>> list(@RequestParam(required = false) String category) {
        if (category != null) {
            try {
                News.Category cat = News.Category.valueOf(category.toUpperCase());
                return Result.ok(newsRepo.findByCategoryOrderByPublishTimeDesc(cat));
            } catch (Exception ignored) {}
        }
        return Result.ok(newsRepo.findAllByOrderByPublishTimeDesc());
    }

    @GetMapping("/{id}")
    public Result<News> getById(@PathVariable Long id) {
        return newsRepo.findById(id).map(Result::ok).orElse(Result.fail("不存在"));
    }

    @PostMapping
    public Result<News> create(@RequestBody News news) {
        return Result.ok(newsRepo.save(news));
    }

    @PutMapping("/{id}")
    public Result<News> update(@PathVariable Long id, @RequestBody News updated) {
        News news = newsRepo.findById(id).orElse(null);
        if (news == null) return Result.fail("不存在");
        news.setTitle(updated.getTitle());
        news.setContent(updated.getContent());
        news.setCategory(updated.getCategory());
        news.setAuthor(updated.getAuthor());
        return Result.ok(newsRepo.save(news));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        newsRepo.deleteById(id);
        return Result.ok();
    }
}
