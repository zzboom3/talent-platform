package com.talent.platform.repository;

import com.talent.platform.entity.LearningCertificate;
import com.talent.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LearningCertificateRepository extends JpaRepository<LearningCertificate, Long> {
    List<LearningCertificate> findByUser(User user);
    Optional<LearningCertificate> findByCertificateNo(String certificateNo);
    Optional<LearningCertificate> findByBlockHash(String blockHash);
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT c FROM LearningCertificate c JOIN FETCH c.user JOIN FETCH c.course ORDER BY c.issueTime DESC")
    List<LearningCertificate> findAllWithDetails();
}
