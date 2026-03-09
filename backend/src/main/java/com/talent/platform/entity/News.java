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

    @CreationTimestamp
    @Column(name = "publish_time", updatable = false)
    private LocalDateTime publishTime;

    public enum Category {
        NEWS, ANNOUNCE, POLICY
    }
}
