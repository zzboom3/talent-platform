package com.talent.platform.repository;

import com.talent.platform.entity.TalentProfile;
import com.talent.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TalentProfileRepository extends JpaRepository<TalentProfile, Long> {
    Optional<TalentProfile> findByUser(User user);
    Optional<TalentProfile> findByUserId(Long userId);
    List<TalentProfile> findBySkillsContaining(String keyword);
    List<TalentProfile> findByVisibleTrue();
    Optional<TalentProfile> findByIdAndVisibleTrue(Long id);
    List<TalentProfile> findByVisibleTrueAndFeaturedTrueOrderByFeaturedOrderAsc();
    long countByVisibleTrue();

    @Query("SELECT t FROM TalentProfile t WHERE t.skills LIKE CONCAT('%', :keyword, '%') OR t.realName LIKE CONCAT('%', :keyword, '%') OR t.education LIKE CONCAT('%', :keyword, '%') OR t.gender LIKE CONCAT('%', :keyword, '%') OR t.graduationSchool LIKE CONCAT('%', :keyword, '%') OR t.major LIKE CONCAT('%', :keyword, '%') OR t.workYears LIKE CONCAT('%', :keyword, '%') OR t.expectedPosition LIKE CONCAT('%', :keyword, '%') OR t.expectedSalary LIKE CONCAT('%', :keyword, '%') OR t.experience LIKE CONCAT('%', :keyword, '%') OR t.projectExperience LIKE CONCAT('%', :keyword, '%') OR t.selfIntroduction LIKE CONCAT('%', :keyword, '%') OR t.certificates LIKE CONCAT('%', :keyword, '%') OR t.city LIKE CONCAT('%', :keyword, '%')")
    List<TalentProfile> findByKeyword(@Param("keyword") String keyword);
}
