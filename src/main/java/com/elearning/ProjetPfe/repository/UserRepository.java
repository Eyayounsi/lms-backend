package com.elearning.ProjetPfe.repository;

import com.elearning.ProjetPfe.entity.Role;
import com.elearning.ProjetPfe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    Optional<User> findByResetToken(String resetToken);

    Optional<User> findByEmailAndOtpCode(String email, String otpCode);

    long countByRole(Role role);

    List<User> findByRoleAndShareWithRecruiters(Role role, Boolean shareWithRecruiters);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.resetToken = null, u.resetTokenExpiry = null WHERE u.resetTokenExpiry < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}