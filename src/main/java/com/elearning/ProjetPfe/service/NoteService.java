package com.elearning.ProjetPfe.service;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elearning.ProjetPfe.entity.*;
import com.elearning.ProjetPfe.repository.*;

@Service
public class NoteService {

    @Autowired private NoteRepository noteRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private LessonRepository lessonRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;

    /** Récupérer la note de cours (sans leçon) */
    public Map<String, Object> getCourseNote(Long courseId, User student) {
        verifyEnrollment(courseId, student);
        Note note = noteRepository.findByStudentIdAndCourseIdAndLessonIsNull(student.getId(), courseId)
                .orElse(null);
        return mapNoteToDto(note, courseId, null);
    }

    /** Sauvegarder/mettre à jour la note de cours */
    @Transactional
    public Map<String, Object> saveCourseNote(Long courseId, Map<String, Object> data, User student) {
        verifyEnrollment(courseId, student);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours introuvable"));

        Note note = noteRepository.findByStudentIdAndCourseIdAndLessonIsNull(student.getId(), courseId)
                .orElseGet(() -> {
                    Note n = new Note();
                    n.setStudent(student);
                    n.setCourse(course);
                    return n;
                });
        note.setContent((String) data.get("content"));
        note = noteRepository.save(note);
        return mapNoteToDto(note, courseId, null);
    }

    /** Récupérer la note d'une leçon */
    public Map<String, Object> getLessonNote(Long lessonId, User student) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Leçon introuvable"));
        verifyEnrollment(lesson.getSection().getCourse().getId(), student);

        Note note = noteRepository.findByStudentIdAndLessonId(student.getId(), lessonId)
                .orElse(null);
        return mapNoteToDto(note, lesson.getSection().getCourse().getId(), lessonId);
    }

    /** Sauvegarder/mettre à jour la note d'une leçon */
    @Transactional
    public Map<String, Object> saveLessonNote(Long lessonId, Map<String, Object> data, User student) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Leçon introuvable"));
        Course course = lesson.getSection().getCourse();
        verifyEnrollment(course.getId(), student);

        Note note = noteRepository.findByStudentIdAndLessonId(student.getId(), lessonId)
                .orElseGet(() -> {
                    Note n = new Note();
                    n.setStudent(student);
                    n.setCourse(course);
                    n.setLesson(lesson);
                    return n;
                });
        note.setContent((String) data.get("content"));
        note = noteRepository.save(note);
        return mapNoteToDto(note, course.getId(), lessonId);
    }

    // ═══════════════════════════════════════════════════════════════════════

    private void verifyEnrollment(Long courseId, User student) {
        enrollmentRepository.findByStudentIdAndCourseIdAndPaymentStatus(
                student.getId(), courseId, PaymentStatus.PAID)
                .orElseThrow(() -> new RuntimeException("Vous n'êtes pas inscrit à ce cours"));
    }

    private Map<String, Object> mapNoteToDto(Note note, Long courseId, Long lessonId) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (note != null) {
            m.put("id", note.getId());
            m.put("content", note.getContent());
            m.put("courseId", courseId);
            m.put("lessonId", lessonId);
            m.put("createdAt", note.getCreatedAt());
            m.put("updatedAt", note.getUpdatedAt());
        } else {
            m.put("id", null);
            m.put("content", "");
            m.put("courseId", courseId);
            m.put("lessonId", lessonId);
            m.put("createdAt", null);
            m.put("updatedAt", null);
        }
        return m;
    }
}
