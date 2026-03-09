package com.talent.platform.repository;

import com.talent.platform.entity.JobApplication;
import com.talent.platform.entity.TalentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findByTalent(TalentProfile talent);
    List<JobApplication> findByJobCompanyId(Long companyId);
    boolean existsByTalentAndJobId(TalentProfile talent, Long jobId);
}
