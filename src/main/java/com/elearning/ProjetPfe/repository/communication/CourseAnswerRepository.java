package com.elearning.ProjetPfe.repository.communication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.communication.CourseAnswer;

@Repository
public interface CourseAnswerRepository extends JpaRepository<CourseAnswer, Long> {

    long countByAuthorId(Long authorId);
}
