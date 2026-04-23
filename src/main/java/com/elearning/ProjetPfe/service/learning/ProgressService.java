package com.elearning.ProjetPfe.service.learning;

import com.elearning.ProjetPfe.entity.course.Course;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elearning.ProjetPfe.dto.learning.CourseProgressDto;
import com.elearning.ProjetPfe.dto.learning.LessonProgressDto;
import com.elearning.ProjetPfe.entity.learning.CourseProgress;
import com.elearning.ProjetPfe.entity.course.Lesson;
import com.elearning.ProjetPfe.entity.learning.LessonProgress;
import com.elearning.ProjetPfe.entity.payment.PaymentStatus;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.learning.CourseProgressRepository;
import com.elearning.ProjetPfe.repository.course.CourseRepository;
import com.elearning.ProjetPfe.repository.payment.EnrollmentRepository;
import com.elearning.ProjetPfe.repository.learning.LessonProgressRepository;
import com.elearning.ProjetPfe.repository.course.LessonRepository;
/**
 * Logique métier pour la progression des étudiants.
 *
 * ═══════════════════════════════════════════════════
 * FLOW COMPLET — "Reprendre où j'en étais"
 * ═══════════════════════════════════════════════════
 *
 *   1. L'étudiant ouvre une leçon vidéo
 *      → GET /api/student/progress/lesson/{lessonId}
 *      → Retourne { watchedSeconds: 245, completed: false }
 *      → Frontend : videoPlayer.currentTime = 245
 *
 *   2. Pendant la lecture, toutes les 30 secondes :
 *      → PUT /api/student/progress/lesson/{lessonId}
 *         Body: { watchedSeconds: 275 }
 *      → Sauvegarde watchedSeconds en base
 *      → Si >= 80% de la durée → completed = true
 *      → Recalcule CourseProgress.completionPercentage
 *
 *   3. Quand completed passe à true :
 *      → Met à jour CourseProgress.lastLessonId
 *      → Si toutes les leçons complètes → CourseProgress.completedAt = now()
 *
 * ═══════════════════════════════════════════════════
 * CALCUL DU POURCENTAGE :
 * ═══════════════════════════════════════════════════
 *
 *   % = (nombre de leçons complétées / nombre total de leçons) * 100
 *
 *   Ex: 3 complétées sur 10 = 30 %
 */
@Service
public class ProgressService {

    @Autowired
    private LessonProgressRepository lessonProgressRepository;

    @Autowired
    private CourseProgressRepository courseProgressRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private com.elearning.ProjetPfe.repository.learning.QuizRepository quizRepository;

    // ═══════════════════════════════════════════════════════════════════════
    //  Obtenir la progression sur une leçon
    // ═══════════════════════════════════════════════════════════════════════

    public LessonProgressDto getLessonProgress(Long lessonId, User student) {
        return lessonProgressRepository
                .findByStudentIdAndLessonId(student.getId(), lessonId)
                .map(this::toLessonDto)
                .orElseGet(() -> {
                    // Aucune progression → retourner 0 secondes
                    LessonProgressDto dto = new LessonProgressDto();
                    dto.setLessonId(lessonId);
                    dto.setWatchedSeconds(0L);
                    dto.setCompleted(false);
                    return dto;
                });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Sauvegarder la progression (appelé toutes les 30 secondes)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public LessonProgressDto saveProgress(Long lessonId, Long watchedSeconds, User student) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Leçon non trouvée"));

        // Vérifier que l'étudiant a acheté le cours
        Long courseId = lesson.getSection().getCourse().getId();
        enrollmentRepository.findByStudentIdAndCourseIdAndPaymentStatus(
                student.getId(), courseId, PaymentStatus.PAID
        ).orElseThrow(() -> new RuntimeException("Accès non autorisé — cours non acheté"));

        // Récupérer ou créer la progression
        LessonProgress progress = lessonProgressRepository
                .findByStudentIdAndLessonId(student.getId(), lessonId)
                .orElse(new LessonProgress());

        progress.setStudent(student);
        progress.setLesson(lesson);
        progress.setWatchedSeconds(watchedSeconds);

        // Marquer comme complété si >= 80 % de la vidéo regardée
        if (!progress.isCompleted()) {
            Long duration = lesson.getDurationSeconds();
            if (duration != null && duration > 0) {
                double percentage = (watchedSeconds * 100.0) / duration;
                if (percentage >= 80.0) {
                    progress.setCompleted(true);
                    progress.setCompletedAt(LocalDateTime.now());
                }
            }
        }

        progress = lessonProgressRepository.save(progress);

        // Recalculer la progression globale du cours
        recalculateCourseProgress(student, courseId, lessonId);

        return toLessonDto(progress);
    }

    /**
     * Marquer une leçon TEXT ou PDF comme terminée manuellement.
     * Appelé quand l'étudiant clique "Marquer comme terminé".
     * 
     * VALIDATION QUIZ OBLIGATOIRE:
     * Si la leçon contient un quiz (hasQuiz = true), l'étudiant DOIT avoir passé
     * le quiz avec succès avant de pouvoir marquer la leçon comme terminée.
     */
    @Transactional
    public LessonProgressDto markLessonCompleted(Long lessonId, User student) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Leçon non trouvée"));

        Long courseId = lesson.getSection().getCourse().getId();

        // ═══ VALIDATION QUIZ OBLIGATOIRE ═══
        // Vérifier si cette leçon a un quiz associé
        if (quizRepository.existsByLessonId(lessonId)) {
            // Récupérer le quiz de la leçon
            com.elearning.ProjetPfe.entity.learning.Quiz quiz = quizRepository.findFirstByLessonId(lessonId).orElse(null);
            if (quiz != null) {
                // Vérifier si l'étudiant a passé le quiz avec succès
                boolean quizPassed = quizRepository.hasStudentPassedQuiz(student.getId(), quiz.getId());
                if (!quizPassed) {
                    throw new RuntimeException("Vous devez réussir le quiz de cette leçon avant de la marquer comme terminée.");
                }
            }
        }

        LessonProgress progress = lessonProgressRepository
                .findByStudentIdAndLessonId(student.getId(), lessonId)
                .orElse(new LessonProgress());

        progress.setStudent(student);
        progress.setLesson(lesson);
        progress.setCompleted(true);
        if (progress.getCompletedAt() == null) {
            progress.setCompletedAt(LocalDateTime.now());
        }

        progress = lessonProgressRepository.save(progress);
        recalculateCourseProgress(student, courseId, lessonId);

        return toLessonDto(progress);
    }

    /**
     * Démarquer une leçon comme non terminée (toggle).
     * Permet à l'étudiant de décocher une leçon marquée comme terminée.
     */
    @Transactional
    public LessonProgressDto markLessonIncomplete(Long lessonId, User student) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Leçon non trouvée"));

        Long courseId = lesson.getSection().getCourse().getId();

        LessonProgress progress = lessonProgressRepository
                .findByStudentIdAndLessonId(student.getId(), lessonId)
                .orElseThrow(() -> new RuntimeException("Aucune progression trouvée pour cette leçon"));

        progress.setCompleted(false);
        progress.setCompletedAt(null);
        // Réinitialiser watchedSeconds pour les vidéos si souhaité
        // progress.setWatchedSeconds(0L);

        progress = lessonProgressRepository.save(progress);
        recalculateCourseProgress(student, courseId, lessonId);

        return toLessonDto(progress);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Obtenir la progression globale d'un cours
    // ═══════════════════════════════════════════════════════════════════════

    public CourseProgressDto getCourseProgress(Long courseId, User student) {
        return courseProgressRepository
                .findByStudentIdAndCourseId(student.getId(), courseId)
                .map(this::toCourseDto)
                .orElseGet(() -> {
                    CourseProgressDto dto = new CourseProgressDto();
                    dto.setCourseId(courseId);
                    dto.setCompletionPercentage(0.0);
                    return dto;
                });
    }

    /**
     * Tous les cours "en cours" d'un étudiant pour le dashboard.
     */
    public List<CourseProgressDto> getMyCourseProgress(User student) {
        return courseProgressRepository
                .findByStudentIdOrderByLastAccessedAtDesc(student.getId())
                .stream()
                .map(this::toCourseDto)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  RECALCUL (privé) — appelé après chaque sauvegarde de leçon
    // ═══════════════════════════════════════════════════════════════════════

    private void recalculateCourseProgress(User student, Long courseId, Long lastLessonId) {
        // Nombre total de leçons dans le cours
        long totalLessons = lessonRepository.countBySectionCourseId(courseId);
        if (totalLessons == 0) return;

        // Nombre de leçons complétées par l'étudiant
        long completedLessons = lessonProgressRepository
                .countCompletedInCourse(student.getId(), courseId);

        double percentage = Math.round((completedLessons * 100.0 / totalLessons) * 10.0) / 10.0;

        // Récupérer ou créer le CourseProgress
        CourseProgress cp = courseProgressRepository
                .findByStudentIdAndCourseId(student.getId(), courseId)
                .orElse(new CourseProgress());

        cp.setStudent(student);
        cp.setCourse(courseRepository.getReferenceById(courseId));
        cp.setCompletionPercentage(percentage);
        cp.setLastLessonId(lastLessonId);
        cp.setLastAccessedAt(LocalDateTime.now());

        // Marquer le cours comme terminé si 100 %
        if (percentage >= 100.0 && cp.getCompletedAt() == null) {
            cp.setCompletedAt(LocalDateTime.now());
            courseProgressRepository.save(cp);
            // Auto-générer le certificat (si pas déjà existant)
            try {
                certificateService.generateCertificate(courseId, student);
            } catch (Exception e) {
                // Ignorer si le certificat existe déjà ou si l'étudiant n'a pas payé
                System.out.println("Info certificat : " + e.getMessage());
            }
        } else {
            courseProgressRepository.save(cp);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CONVERSIONS
    // ═══════════════════════════════════════════════════════════════════════

    private LessonProgressDto toLessonDto(LessonProgress lp) {
        LessonProgressDto dto = new LessonProgressDto();
        dto.setLessonId(lp.getLesson().getId());
        dto.setWatchedSeconds(lp.getWatchedSeconds());
        dto.setCompleted(lp.isCompleted());
        dto.setCompletedAt(lp.getCompletedAt());
        dto.setUpdatedAt(lp.getUpdatedAt());
        return dto;
    }

    private CourseProgressDto toCourseDto(CourseProgress cp) {
        CourseProgressDto dto = new CourseProgressDto();
        dto.setCourseId(cp.getCourse().getId());
        dto.setCourseTitle(cp.getCourse().getTitle());
        dto.setCourseCoverImage(cp.getCourse().getCoverImage());
        dto.setCompletionPercentage(cp.getCompletionPercentage());
        dto.setLastLessonId(cp.getLastLessonId());
        dto.setLastAccessedAt(cp.getLastAccessedAt());
        dto.setCompletedAt(cp.getCompletedAt());
        dto.setCompleted(cp.getCompletedAt() != null);
        dto.setCompletedLessonIds(
            lessonProgressRepository.findCompletedLessonIds(cp.getStudent().getId(), cp.getCourse().getId())
        );
        return dto;
    }
}
