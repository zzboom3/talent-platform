package com.talent.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "talent_profile")
public class TalentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "real_name", length = 50)
    private String realName;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(length = 100)
    private String education;

    @Column(length = 20)
    private String gender;

    @Column(length = 100)
    private String graduationSchool;

    @Column(length = 100)
    private String major;

    @Column(length = 20)
    private String workYears;

    @Column(length = 100)
    private String expectedPosition;

    @Column(length = 50)
    private String expectedSalary;

    @Column(columnDefinition = "TEXT")
    private String experience;

    @Column(columnDefinition = "TEXT")
    private String projectExperience;

    @Column(columnDefinition = "TEXT")
    private String selfIntroduction;

    @Column(columnDefinition = "TEXT")
    private String certificates;

    @Column(length = 50)
    private String city;

    @Column(length = 200)
    private String avatarUrl;

    @Column(length = 20)
    private String status = "ACTIVE";

    @Column(nullable = false)
    private Boolean featured = false;

    @Column(name = "featured_order", columnDefinition = "INT DEFAULT 0")
    private Integer featuredOrder = 0;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean visible = true;
}
