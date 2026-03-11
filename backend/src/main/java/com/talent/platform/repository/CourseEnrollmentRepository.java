package com.talent.platform.repository;

import com.talent.platform.entity.CourseEnrollment;
import com.talent.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {
    List<CourseEnrollment> findByUser(User user);
    boolean existsByUserAndCourseId(User user, Long courseId);
    Optional<CourseEnrollment> findByUserAndCourseId(User user, Long courseId);

    @Query("""
            SELECT YEAR(e.enrollTime), MONTH(e.enrollTime), COUNT(e)
            FROM CourseEnrollment e
            WHERE e.enrollTime >= :startTime
            GROUP BY YEAR(e.enrollTime), MONTH(e.enrollTime)
            ORDER BY YEAR(e.enrollTime), MONTH(e.enrollTime)
            """)
    List<Object[]> countMonthlyEnrollments(@Param("startTime") LocalDateTime startTime);
}
