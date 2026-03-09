package com.talent.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "company")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(length = 50)
    private String industry;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;
}
