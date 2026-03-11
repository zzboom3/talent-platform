package com.talent.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "api_access_log")
public class ApiAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10)
    private String method;

    @Column(length = 500)
    private String uri;

    @Column(length = 50)
    private String username;

    @Column(length = 50)
    private String ip;

    @Column(name = "cost_ms")
    private Long costMs;

    @Column(name = "status_code")
    private Integer statusCode;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
