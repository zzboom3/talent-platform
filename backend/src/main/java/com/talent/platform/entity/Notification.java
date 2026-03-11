package com.talent.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Type type;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "is_read", nullable = false)
    private Boolean read = false;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    public enum Type {
        APPLICATION_NEW,
        APPLICATION_ACCEPTED,
        APPLICATION_REJECTED,
        COMPANY_PENDING_AUDIT,
        COMPANY_AUDIT_RESULT,
        JOB_CLOSED,
        JOB_NEW,
        TALENT_FEATURED,
        CERTIFICATE_ISSUED,
        NEWS_PENDING_REVIEW,
        NEW_USER_REGISTER,
        WELCOME
    }
}
