package com.elearning.ProjetPfe.repository.learning;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.learning.Choice;

@Repository
public interface ChoiceRepository extends JpaRepository<Choice, Long> {
}
