package com.elearning.ProjetPfe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.CourseAnswer;

@Repository
public interface CourseAnswerRepository extends JpaRepository<CourseAnswer, Long> {
}
