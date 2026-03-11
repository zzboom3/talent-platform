package com.talent.platform.repository;

import com.talent.platform.entity.ApiAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ApiAccessLogRepository extends JpaRepository<ApiAccessLog, Long> {

    long countByCreateTimeAfter(LocalDateTime time);

    @Query("SELECT a.uri, COUNT(a) as cnt FROM ApiAccessLog a GROUP BY a.uri ORDER BY cnt DESC")
    List<Object[]> findTopUris();

    @Query("SELECT AVG(a.costMs) FROM ApiAccessLog a WHERE a.createTime > :since")
    Double avgCostSince(LocalDateTime since);

    @Query("SELECT COUNT(a) FROM ApiAccessLog a WHERE a.statusCode >= 400 AND a.createTime > :since")
    Long countErrorsSince(LocalDateTime since);

    /** 近7天每日调用量：日期字符串, 调用次数 */
    @Query(value = "SELECT DATE(create_time) as d, COUNT(*) as c FROM api_access_log WHERE create_time >= :since GROUP BY DATE(create_time) ORDER BY d", nativeQuery = true)
    List<Object[]> findDailyCountsSince(@Param("since") LocalDateTime since);

    /** 今日每小时调用量：小时(0-23), 调用次数 */
    @Query(value = "SELECT HOUR(create_time) as h, COUNT(*) as c FROM api_access_log WHERE create_time >= :since GROUP BY HOUR(create_time) ORDER BY h", nativeQuery = true)
    List<Object[]> findHourlyCountsSince(@Param("since") LocalDateTime since);

    /** 今日成功/失败调用数 */
    @Query("SELECT COUNT(a) FROM ApiAccessLog a WHERE a.createTime >= :since AND (a.statusCode IS NULL OR a.statusCode < 400)")
    Long countSuccessSince(@Param("since") LocalDateTime since);
}
