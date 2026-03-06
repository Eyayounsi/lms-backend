package com.elearning.ProjetPfe.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.Question;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByQuizIdOrderByOrderIndexAsc(Long quizId);

    long countByQuizId(Long quizId);
}
