package com.elearning.ProjetPfe.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.elearning.ProjetPfe.dto.CourseResponseDto;
import com.elearning.ProjetPfe.dto.CreateCourseDto;
import com.elearning.ProjetPfe.dto.InstructorStudentDto;
import com.elearning.ProjetPfe.dto.LessonDto;
import com.elearning.ProjetPfe.dto.ResourceDto;
import com.elearning.ProjetPfe.dto.SectionDto;
import com.elearning.ProjetPfe.entity.Enrollment;
import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.repository.CourseProgressRepository;
import com.elearning.ProjetPfe.repository.EnrollmentRepository;
import com.elearning.ProjetPfe.service.CourseService;
import com.elearning.ProjetPfe.service.ResourceService;

/**
 * Endpoints pour l'instructor — gestion de SES cours.
 *
 * Toutes les URLs commencent par /api/instructor/courses
 * Sécurisé par SecurityConfig : hasAuthority("INSTRUCTOR")
 */
@RestController
@RequestMapping("/api/instructor/courses")
public class InstructorCourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseProgressRepository courseProgressRepository;

    // ─── CRÉER un cours ──────────────────────────────────────────────────
    // POST /api/instructor/courses
    @PostMapping
    public ResponseEntity<CourseResponseDto> createCourse(
            @RequestBody CreateCourseDto dto,
            @AuthenticationPrincipal User instructor) {
        return ResponseEntity.ok(courseService.createCourse(dto, instructor));
    }

    // ─── MODIFIER un cours ───────────────────────────────────────────────
    // PUT /api/instructor/courses/5
    @PutMapping("/{courseId}")
    public ResponseEntity<CourseResponseDto> updateCourse(
            @PathVariable Long courseId,
            @RequestBody CreateCourseDto dto,
            @AuthenticationPrincipal User instructor) {
        return ResponseEntity.ok(courseService.updateCourse(courseId, dto, instructor));
    }

    // ─── SUPPRIMER un cours ──────────────────────────────────────────────
    // DELETE /api/instructor/courses/5
    @DeleteMapping("/{courseId}")
    public ResponseEntity<String> deleteCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User instructor) {
        courseService.deleteCourse(courseId, instructor);
        return ResponseEntity.ok("Cours supprimé avec succès");
    }

    // ─── SOUMETTRE pour validation ───────────────────────────────────────
    // PUT /api/instructor/courses/5/submit
    @PutMapping("/{courseId}/submit")
    public ResponseEntity<CourseResponseDto> submitForReview(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User instructor) {
        return ResponseEntity.ok(courseService.submitForReview(courseId, instructor));
    }

    // ─── MES COURS ───────────────────────────────────────────────────────
    // GET /api/instructor/courses
    @GetMapping
    public ResponseEntity<List<CourseResponseDto>> getMyCourses(
            @AuthenticationPrincipal User instructor) {
        return ResponseEntity.ok(courseService.getMyCourses(instructor));
    }

    // ─── VOIR UN COURS ───────────────────────────────────────────────────
    // GET /api/instructor/courses/5
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseResponseDto> getCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseById(courseId));
    }

    // ─── AJOUTER une section ─────────────────────────────────────────────
    // POST /api/instructor/courses/5/sections
    @PostMapping("/{courseId}/sections")
    public ResponseEntity<SectionDto> addSection(
            @PathVariable Long courseId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User instructor) {
        String title = body.get("title");
        return ResponseEntity.ok(courseService.addSection(courseId, title, instructor));
    }

    // ─── AJOUTER une leçon ──────────────────────────────────────────────
    // POST /api/instructor/courses/sections/10/lessons
    @PostMapping("/sections/{sectionId}/lessons")
    public ResponseEntity<LessonDto> addLesson(
            @PathVariable Long sectionId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User instructor) {
        String title = (String) body.get("title");
        String lessonType = body.get("lessonType") != null ? (String) body.get("lessonType") : "VIDEO";
        boolean isFree = body.get("isFree") != null && Boolean.parseBoolean(body.get("isFree").toString());
        return ResponseEntity.ok(courseService.addLesson(sectionId, title, lessonType, isFree, instructor));
    }

    // ─── UPLOAD image de couverture ──────────────────────────────────────
    // POST /api/instructor/courses/5/cover
    @PostMapping("/{courseId}/cover")
    public ResponseEntity<CourseResponseDto> uploadCover(
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User instructor) {
        return ResponseEntity.ok(courseService.uploadCoverImage(courseId, file, instructor));
    }

    // ─── UPLOAD vidéo d'une leçon ────────────────────────────────────────
    // POST /api/instructor/courses/lessons/10/video
    @PostMapping("/lessons/{lessonId}/video")
    public ResponseEntity<LessonDto> uploadVideo(
            @PathVariable Long lessonId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User instructor) {
        return ResponseEntity.ok(courseService.uploadLessonVideo(lessonId, file, instructor));
    }

    // ─── UPLOAD PDF d'une leçon ──────────────────────────────────────────
    // POST /api/instructor/courses/lessons/10/pdf
    @PostMapping("/lessons/{lessonId}/pdf")
    public ResponseEntity<LessonDto> uploadPdf(
            @PathVariable Long lessonId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User instructor) {
        return ResponseEntity.ok(courseService.uploadLessonPdf(lessonId, file, instructor));
    }

    // ─── ARCHIVER un cours (PUBLISHED → ARCHIVED) ────────────────────────
    // PUT /api/instructor/courses/5/archive
    @PutMapping("/{courseId}/archive")
    public ResponseEntity<CourseResponseDto> archiveCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User instructor) {
        return ResponseEntity.ok(courseService.archiveCourse(courseId, instructor));
    }

    // ─── RESSOURCES d'une leçon ───────────────────────────────────────────
    // GET /api/instructor/courses/lessons/10/resources
    @GetMapping("/lessons/{lessonId}/resources")
    public ResponseEntity<List<ResourceDto>> getResources(
            @PathVariable Long lessonId) {
        return ResponseEntity.ok(resourceService.getByLesson(lessonId));
    }

    // POST /api/instructor/courses/lessons/10/resources
    @PostMapping("/lessons/{lessonId}/resources")
    public ResponseEntity<ResourceDto> addResource(
            @PathVariable Long lessonId,
            @RequestBody ResourceDto dto,
            @AuthenticationPrincipal User instructor) {
        return ResponseEntity.ok(resourceService.addResource(lessonId, dto, instructor));
    }

    // DELETE /api/instructor/courses/lessons/resources/55
    @DeleteMapping("/lessons/resources/{resourceId}")
    public ResponseEntity<String> deleteResource(
            @PathVariable Long resourceId,
            @AuthenticationPrincipal User instructor) {
        resourceService.deleteResource(resourceId, instructor);
        return ResponseEntity.ok("Ressource supprimée");
    }
    // ─── MODIFIER une leçon (titre, accès) ────────────────────────────────────
    // PUT /api/instructor/courses/lessons/{lessonId}
    @PutMapping("/lessons/{lessonId}")
    public ResponseEntity<LessonDto> updateLesson(
            @PathVariable Long lessonId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User instructor) {
        return ResponseEntity.ok(courseService.updateLesson(lessonId, body, instructor));
    }

    // ─── SUPPRIMER une leçon ──────────────────────────────────────────────
    // DELETE /api/instructor/courses/lessons/{lessonId}
    @DeleteMapping("/lessons/{lessonId}")
    public ResponseEntity<String> deleteLesson(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal User instructor) {
        courseService.deleteLesson(lessonId, instructor);
        return ResponseEntity.ok("Leçon supprimée");
    }
    // ─── SUPPRESSION du contenu d'une leçon ─────────────────────────────
    // DELETE /api/instructor/courses/lessons/{lessonId}/video
    @DeleteMapping("/lessons/{lessonId}/video")
    public ResponseEntity<LessonDto> clearVideo(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal User instructor) {
        return ResponseEntity.ok(courseService.clearLessonVideo(lessonId, instructor));
    }

    // DELETE /api/instructor/courses/lessons/{lessonId}/pdf
    @DeleteMapping("/lessons/{lessonId}/pdf")
    public ResponseEntity<LessonDto> clearPdf(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal User instructor) {
        return ResponseEntity.ok(courseService.clearLessonPdf(lessonId, instructor));
    }

    // DELETE /api/instructor/courses/lessons/{lessonId}/article
    @DeleteMapping("/lessons/{lessonId}/article")
    public ResponseEntity<LessonDto> clearArticle(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal User instructor) {
        return ResponseEntity.ok(courseService.clearLessonArticle(lessonId, instructor));
    }

    // ─── REVIEWS D'UN COURS DE L'INSTRUCTOR ──────────────────────────────
    // GET /api/instructor/courses/5/reviews
    @GetMapping("/{courseId}/reviews")
    public ResponseEntity<List<Map<String, Object>>> getCourseReviews(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User instructor) {
        return ResponseEntity.ok(courseService.getInstructorCourseReviews(courseId, instructor));
    }

    // ─── CONTENU ARTICLE D'UNE LEÇON ────────────────────────────────────
    // PUT /api/instructor/courses/lessons/{lessonId}/article
    @PutMapping("/lessons/{lessonId}/article")
    public ResponseEntity<LessonDto> saveArticleContent(
            @PathVariable Long lessonId,
            @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal User instructor) {
        String content = body.getOrDefault("content", "");
        return ResponseEntity.ok(courseService.saveArticleContent(lessonId, content, instructor));
    }

    // ─── LISTE DES ÉTUDIANTS DE L'INSTRUCTOR ─────────────────────────────
    // GET /api/instructor/courses/students
    @GetMapping("/students")
    public ResponseEntity<List<InstructorStudentDto>> getMyStudents(
            @AuthenticationPrincipal User instructor) {
        List<Enrollment> enrollments = enrollmentRepository.findByInstructorId(instructor.getId());
        List<InstructorStudentDto> result = enrollments.stream().map(e -> {
            InstructorStudentDto dto = new InstructorStudentDto();
            dto.setEnrollmentId(e.getId());
            dto.setStudentId(e.getStudent().getId());
            dto.setStudentName(e.getStudent().getFullName());
            dto.setStudentEmail(e.getStudent().getEmail());
            dto.setStudentAvatar(e.getStudent().getAvatarPath());
            dto.setCourseId(e.getCourse().getId());
            dto.setCourseTitle(e.getCourse().getTitle());
            dto.setEnrolledAt(e.getCreatedAt());

            // Récupérer la progression
            courseProgressRepository.findByStudentIdAndCourseId(
                    e.getStudent().getId(), e.getCourse().getId()
            ).ifPresent(cp -> {
                dto.setCompletionPercentage(cp.getCompletionPercentage());
                dto.setCompleted(cp.getCompletedAt() != null);
                dto.setCompletedAt(cp.getCompletedAt());
            });

            return dto;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }
}

