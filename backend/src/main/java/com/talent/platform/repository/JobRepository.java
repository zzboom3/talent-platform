package com.talent.platform.repository;

import com.talent.platform.entity.Company;
import com.talent.platform.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByCompany(Company company);
    boolean existsByTitleAndCompanyId(String title, Long companyId);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.company.id = :companyId")
    long countByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.company.user.username = :username")
    long countByCrawlerUsername(@Param("username") String username);

    @Query(value = """
            SELECT COUNT(*) FROM job j
            LEFT JOIN company c ON j.company_id = c.id
            LEFT JOIN sys_user u ON c.user_id = u.id
            WHERE u.username = :username
               OR j.description LIKE CONCAT('%', :descriptionMarker, '%')
               OR j.requirements LIKE CONCAT('%', :requirementsMarker, '%')
            """, nativeQuery = true)
    long countCrawledJobs(@Param("username") String username,
                          @Param("descriptionMarker") String descriptionMarker,
                          @Param("requirementsMarker") String requirementsMarker);
    long countByStatus(String status);
    long countByCreateTimeAfter(java.time.LocalDateTime createTime);

    @Query("SELECT j FROM Job j WHERE j.requirements LIKE CONCAT('%', :keyword, '%') OR j.title LIKE CONCAT('%', :keyword, '%') OR j.description LIKE CONCAT('%', :keyword, '%')")
    List<Job> findByKeyword(@Param("keyword") String keyword);
}
