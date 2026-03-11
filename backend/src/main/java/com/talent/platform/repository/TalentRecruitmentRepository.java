package com.talent.platform.repository;

import com.talent.platform.entity.TalentRecruitment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TalentRecruitmentRepository extends JpaRepository<TalentRecruitment, Long> {
    List<TalentRecruitment> findByCompanyId(Long companyId);
    List<TalentRecruitment> findByStatusOrderByCreateTimeDesc(TalentRecruitment.Status status);
    List<TalentRecruitment> findAllByOrderByCreateTimeDesc();
}
