package com.elearning.ProjetPfe.controller.analytics;

import com.elearning.ProjetPfe.entity.auth.Role;
import com.elearning.ProjetPfe.entity.course.Course;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.auth.UserRepository;
import com.elearning.ProjetPfe.repository.course.CourseRepository;
import com.elearning.ProjetPfe.service.engagement.DetectionRemarkService;

@RestController
public class DetectionRemarkController {

    private final DetectionRemarkService remarkService;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public DetectionRemarkController(DetectionRemarkService remarkService,
                                     UserRepository userRepository,
                                     CourseRepository courseRepository) {
        this.remarkService = remarkService;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    private User resolveUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

    // ═══ Student endpoints (authenticated) ═══

    /**
     * POST /api/detection-remarks
     * Student saves a detection remark from camera.
     */
    @PostMapping("/api/detection-remarks")
    public ResponseEntity<?> saveRemark(@RequestBody Map<String, Object> body,
                                        Authentication authentication) {
        User student = resolveUser(authentication);
        Long courseId = Long.valueOf(body.get("courseId").toString());
        Long lessonId = body.get("lessonId") != null ? Long.valueOf(body.get("lessonId").toString()) : null;
        String remarkType = (String) body.get("remarkType");
        String remarkMessage = (String) body.get("remarkMessage");
        Integer attentionScore = body.get("attentionScore") != null
                ? Integer.valueOf(body.get("attentionScore").toString()) : null;

        remarkService.saveRemark(student.getId(), courseId, lessonId, remarkType, remarkMessage, attentionScore);
        return ResponseEntity.ok(Map.of("status", "saved"));
    }

    /**
     * GET /api/detection-remarks/my?courseId=X
     * Student gets their own remarks for a course.
     */
    @GetMapping("/api/detection-remarks/my")
    public ResponseEntity<List<Map<String, Object>>> getMyRemarks(
            @RequestParam Long courseId, Authentication authentication) {
        User student = resolveUser(authentication);
        return ResponseEntity.ok(remarkService.getRemarksByStudentAndCourse(student.getId(), courseId));
    }

    // ═══ Admin endpoints (ADMIN role via SecurityConfig /api/admin/**) ═══

    @GetMapping("/api/admin/detection-remarks/summary")
    public ResponseEntity<List<Map<String, Object>>> getAdminSummary() {
        return ResponseEntity.ok(remarkService.getAllRemarksSummary());
    }

    @GetMapping("/api/admin/detection-remarks/by-student")
    public ResponseEntity<List<Map<String, Object>>> getByStudent(@RequestParam Long studentId) {
        return ResponseEntity.ok(remarkService.getRemarksByStudent(studentId));
    }

    @GetMapping("/api/admin/detection-remarks/by-course")
    public ResponseEntity<List<Map<String, Object>>> adminByCourse(@RequestParam Long courseId) {
        return ResponseEntity.ok(remarkService.getRemarksByCourse(courseId));
    }

    @GetMapping("/api/admin/detection-remarks/by-student-course")
    public ResponseEntity<List<Map<String, Object>>> adminByStudentAndCourse(
            @RequestParam Long studentId, @RequestParam Long courseId) {
        return ResponseEntity.ok(remarkService.getRemarksByStudentAndCourse(studentId, courseId));
    }

    // ═══ Instructor endpoints (INSTRUCTOR role via SecurityConfig /api/instructor/**) ═══

    @GetMapping("/api/instructor/detection-remarks/by-course")
    public ResponseEntity<List<Map<String, Object>>> instructorByCourse(
            @RequestParam Long courseId, Authentication authentication) {
        User instructor = resolveUser(authentication);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours introuvable"));
        if (!course.getInstructor().getId().equals(instructor.getId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(remarkService.getRemarksByCourse(courseId));
    }

    @GetMapping("/api/instructor/detection-remarks/by-student-course")
    public ResponseEntity<List<Map<String, Object>>> instructorByStudentAndCourse(
            @RequestParam Long studentId, @RequestParam Long courseId, Authentication authentication) {
        User instructor = resolveUser(authentication);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours introuvable"));
        if (!course.getInstructor().getId().equals(instructor.getId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(remarkService.getRemarksByStudentAndCourse(studentId, courseId));
    }
}
