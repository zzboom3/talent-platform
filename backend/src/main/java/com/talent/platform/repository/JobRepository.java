package com.talent.platform.repository;

import com.talent.platform.entity.Company;
import com.talent.platform.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByCompany(Company company);

    @Query("SELECT j FROM Job j WHERE j.requirements LIKE %:keyword% OR j.title LIKE %:keyword% OR j.description LIKE %:keyword%")
    List<Job> findByKeyword(String keyword);
}
