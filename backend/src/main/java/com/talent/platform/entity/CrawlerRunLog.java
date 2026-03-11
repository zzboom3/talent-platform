package com.talent.platform.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "crawler_run_log")
public class CrawlerRunLog {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at", nullable = false)
    private LocalDateTime finishedAt;

    @Column(name = "jobs_inserted", nullable = false)
    private int jobsInserted;

    @Column(name = "news_inserted", nullable = false)
    private int newsInserted;

    @Column(name = "total_inserted", nullable = false)
    private int totalInserted;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "messages", columnDefinition = "TEXT")
    private String messagesJson;

    public List<String> getMessages() {
        if (messagesJson == null || messagesJson.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(messagesJson, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    public void setMessages(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            this.messagesJson = "[]";
            return;
        }
        try {
            this.messagesJson = OBJECT_MAPPER.writeValueAsString(messages);
        } catch (Exception e) {
            this.messagesJson = "[]";
        }
    }
}
