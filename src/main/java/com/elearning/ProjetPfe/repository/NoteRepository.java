package com.elearning.ProjetPfe.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.Note;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    /** Note de cours globale (lesson = null) */
    Optional<Note> findByStudentIdAndCourseIdAndLessonIsNull(Long studentId, Long courseId);

    /** Note d'une leçon spécifique */
    Optional<Note> findByStudentIdAndLessonId(Long studentId, Long lessonId);
}
