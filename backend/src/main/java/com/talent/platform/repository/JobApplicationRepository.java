package com.talent.platform.repository;

import com.talent.platform.entity.Job;
import com.talent.platform.entity.JobApplication;
import com.talent.platform.entity.TalentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findByTalent(TalentProfile talent);
    List<JobApplication> findByJob(Job job);
    List<JobApplication> findByJob_Company_Id(Long companyId);
    boolean existsByTalentAndJobId(TalentProfile talent, Long jobId);

    @Query("""
            SELECT YEAR(a.applyTime), MONTH(a.applyTime), COUNT(a)
            FROM JobApplication a
            WHERE a.applyTime >= :startTime
            GROUP BY YEAR(a.applyTime), MONTH(a.applyTime)
            ORDER BY YEAR(a.applyTime), MONTH(a.applyTime)
            """)
    List<Object[]> countMonthlyApplications(@Param("startTime") LocalDateTime startTime);

    @Query("""
            SELECT a.status, COUNT(a)
            FROM JobApplication a
            GROUP BY a.status
            """)
    List<Object[]> countByStatus();
}
