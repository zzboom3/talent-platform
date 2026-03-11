package com.talent.platform.repository;

import com.talent.platform.entity.CareerAssessment;
import com.talent.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CareerAssessmentRepository extends JpaRepository<CareerAssessment, Long> {
    List<CareerAssessment> findByUserOrderByCreateTimeDesc(User user);
}
