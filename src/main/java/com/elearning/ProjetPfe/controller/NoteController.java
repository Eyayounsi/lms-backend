package com.elearning.ProjetPfe.controller;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.repository.UserRepository;
import com.elearning.ProjetPfe.service.NoteService;

/**
 * Endpoints notes — pour les étudiants inscrits.
 * Sécurisé par SecurityConfig : anyRequest().authenticated()
 */
@RestController
@RequestMapping("/api/student/notes")
public class NoteController {

    @Autowired private NoteService noteService;
    @Autowired private UserRepository userRepository;

    private User getStudent(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    /** GET /api/student/notes/course/{courseId} — Note de cours */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<Map<String, Object>> getCourseNote(
            @PathVariable Long courseId, Authentication auth) {
        return ResponseEntity.ok(noteService.getCourseNote(courseId, getStudent(auth)));
    }

    /** PUT /api/student/notes/course/{courseId} — Sauvegarder note de cours */
    @PutMapping("/course/{courseId}")
    public ResponseEntity<Map<String, Object>> saveCourseNote(
            @PathVariable Long courseId,
            @RequestBody Map<String, Object> data, Authentication auth) {
        return ResponseEntity.ok(noteService.saveCourseNote(courseId, data, getStudent(auth)));
    }

    /** GET /api/student/notes/lesson/{lessonId} — Note de leçon */
    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<Map<String, Object>> getLessonNote(
            @PathVariable Long lessonId, Authentication auth) {
        return ResponseEntity.ok(noteService.getLessonNote(lessonId, getStudent(auth)));
    }

    /** PUT /api/student/notes/lesson/{lessonId} — Sauvegarder note de leçon */
    @PutMapping("/lesson/{lessonId}")
    public ResponseEntity<Map<String, Object>> saveLessonNote(
            @PathVariable Long lessonId,
            @RequestBody Map<String, Object> data, Authentication auth) {
        return ResponseEntity.ok(noteService.saveLessonNote(lessonId, data, getStudent(auth)));
    }
}
