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

    @Column(columnDefinition = "TEXT")
    private String experience;

    @Column(length = 50)
    private String city;

    @Column(length = 200)
    private String avatarUrl;

    @Column(length = 20)
    private String status = "ACTIVE";
}
