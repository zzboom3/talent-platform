package com.talent.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "learning_certificate")
public class LearningCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "certificate_no", unique = true, nullable = false, length = 50)
    private String certificateNo;

    @Column(name = "block_hash", length = 64)
    private String blockHash;

    @CreationTimestamp
    @Column(name = "issue_time", updatable = false)
    private LocalDateTime issueTime;

    @Column(length = 20)
    private String status = "VALID";
}
