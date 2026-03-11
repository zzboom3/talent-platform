package com.talent.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "news")
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Category category = Category.NEWS;

    @Column(length = 50)
    private String author;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 20, nullable = false)
    private SourceType sourceType = SourceType.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", length = 20, nullable = false)
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;

    @Column(name = "source_site", length = 100)
    private String sourceSite;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by", length = 50)
    private String reviewedBy;

    @CreationTimestamp
    @Column(name = "publish_time", updatable = false)
    private LocalDateTime publishTime;

    public enum Category {
        NEWS, ANNOUNCE, POLICY
    }

    public enum SourceType {
        MANUAL, CRAWLED
    }

    public enum ReviewStatus {
        PENDING, APPROVED, REJECTED
    }
}
