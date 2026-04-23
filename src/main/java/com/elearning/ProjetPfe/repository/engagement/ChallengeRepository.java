package com.elearning.ProjetPfe.repository.engagement;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.engagement.Challenge;
import com.elearning.ProjetPfe.entity.engagement.ChallengeCode;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    List<Challenge> findByStudentIdOrderByChallengeCodeAsc(Long studentId);

    Optional<Challenge> findByStudentIdAndChallengeCode(Long studentId, ChallengeCode challengeCode);

    boolean existsByStudentId(Long studentId);

    void deleteByStudentId(Long studentId);
}
