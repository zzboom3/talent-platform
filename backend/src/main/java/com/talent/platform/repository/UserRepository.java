package com.talent.platform.repository;

import com.talent.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByRole(User.Role role);

    @Query("""
            SELECT YEAR(u.createTime), MONTH(u.createTime), COUNT(u)
            FROM User u
            WHERE u.createTime >= :startTime
            GROUP BY YEAR(u.createTime), MONTH(u.createTime)
            ORDER BY YEAR(u.createTime), MONTH(u.createTime)
            """)
    List<Object[]> countMonthlyRegistrations(@Param("startTime") LocalDateTime startTime);
}
