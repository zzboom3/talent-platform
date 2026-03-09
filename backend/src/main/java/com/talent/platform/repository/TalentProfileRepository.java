package com.talent.platform.repository;

import com.talent.platform.entity.TalentProfile;
import com.talent.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TalentProfileRepository extends JpaRepository<TalentProfile, Long> {
    Optional<TalentProfile> findByUser(User user);
    Optional<TalentProfile> findByUserId(Long userId);
    List<TalentProfile> findBySkillsContaining(String keyword);
}
