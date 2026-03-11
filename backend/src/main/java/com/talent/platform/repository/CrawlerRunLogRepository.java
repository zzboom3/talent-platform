package com.talent.platform.repository;

import com.talent.platform.entity.CrawlerRunLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrawlerRunLogRepository extends JpaRepository<CrawlerRunLog, Long> {

    Optional<CrawlerRunLog> findTop1ByOrderByFinishedAtDesc();
}
