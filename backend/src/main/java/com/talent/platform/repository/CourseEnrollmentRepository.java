package com.talent.platform.repository;

import com.talent.platform.entity.CourseEnrollment;
import com.talent.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {
    List<CourseEnrollment> findByUser(User user);
    boolean existsByUserAndCourseId(User user, Long courseId);
    Optional<CourseEnrollment> findByUserAndCourseId(User user, Long courseId);
}
