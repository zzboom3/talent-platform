package com.talent.platform.repository;

import com.talent.platform.entity.Company;
import com.talent.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByUser(User user);
    Optional<Company> findByUserId(Long userId);
    Optional<Company> findByCompanyName(String companyName);
    long countByVisibleTrue();
}
