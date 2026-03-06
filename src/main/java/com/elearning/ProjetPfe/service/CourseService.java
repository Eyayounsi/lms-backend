package com.elearning.ProjetPfe.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.elearning.ProjetPfe.dto.CourseResponseDto;
import com.elearning.ProjetPfe.dto.CourseReviewDto;
import com.elearning.ProjetPfe.dto.CreateCourseDto;
import com.elearning.ProjetPfe.dto.LessonDto;
import com.elearning.ProjetPfe.dto.SectionDto;
import com.elearning.ProjetPfe.entity.Category;
import com.elearning.ProjetPfe.entity.Course;
import com.elearning.ProjetPfe.entity.CourseLevel;
import com.elearning.ProjetPfe.entity.CourseStatus;
import com.elearning.ProjetPfe.entity.Lesson;
import com.elearning.ProjetPfe.entity.NotificationType;
import com.elearning.ProjetPfe.entity.PaymentStatus;
import com.elearning.ProjetPfe.entity.Section;
import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.repository.CategoryRepository;
import com.elearning.ProjetPfe.repository.CartItemRepository;
import com.elearning.ProjetPfe.repository.CourseProgressRepository;
import com.elearning.ProjetPfe.repository.CourseRepository;
import com.elearning.ProjetPfe.repository.EnrollmentRepository;
import com.elearning.ProjetPfe.repository.LessonProgressRepository;
import com.elearning.ProjetPfe.repository.LessonRepository;
import com.elearning.ProjetPfe.repository.ResourceRepository;
import com.elearning.ProjetPfe.repository.ReviewRepository;
import com.elearning.ProjetPfe.repository.SectionRepository;
import com.elearning.ProjetPfe.repository.WishlistItemRepository;

/**
 * Toute la logique métier des cours.
 *
 * Workflow :
 *   1. Instructor crée un cours (DRAFT)
 *   2. Instructor ajoute sections + leçons
 *   3. Instructor soumet le cours (PENDING)
 *   4. Admin consulte les cours PENDING
 *   5. Admin accepte (PUBLISHED) ou rejette (REJECTED) + email envoyé
 *   6. Student voit les cours PUBLISHED
 */
@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private WishlistItemRepository wishlistItemRepository;

    @Autowired
    private CourseProgressRepository courseProgressRepository;

    @Autowired
    private LessonProgressRepository lessonProgressRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private com.elearning.ProjetPfe.repository.QuizRepository quizRepository;

    // ═══════════════════════════════════════════════════════════════════════
    //  INSTRUCTOR — Créer un cours
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public CourseResponseDto createCourse(CreateCourseDto dto, User instructor) {
        Course course = new Course();
        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setPrice(dto.getPrice());
        course.setStatus(CourseStatus.DRAFT);  // toujours DRAFT à la création
        course.setInstructor(instructor);

        // Niveau du cours
        try {
            course.setLevel(CourseLevel.valueOf(dto.getLevel().toUpperCase()));
        } catch (Exception e) {
            course.setLevel(CourseLevel.BEGINNER);
        }

        // Nouveaux champs
        if (dto.getObjectives() != null) course.setObjectives(dto.getObjectives());
        if (dto.getRequirements() != null) course.setRequirements(dto.getRequirements());
        if (dto.getLanguage() != null) course.setLanguage(dto.getLanguage());
        if (dto.getDiscountPrice() != null) course.setDiscountPrice(dto.getDiscountPrice());
        if (dto.getDiscountEndsAt() != null) course.setDiscountEndsAt(dto.getDiscountEndsAt());

        // Catégorie (optionnelle)
        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
            course.setCategory(category);
        }

        course = courseRepository.save(course);
        return toResponseDto(course);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INSTRUCTOR — Modifier un cours (DRAFT, REJECTED → direct, PUBLISHED → pending edit)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public CourseResponseDto updateCourse(Long courseId, CreateCourseDto dto, User instructor) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        // Sécurité : seul le propriétaire peut modifier
        if (!course.getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Vous n'êtes pas le propriétaire de ce cours");
        }

        // On peut modifier un cours DRAFT, REJECTED ou PUBLISHED
        // (PENDING et ARCHIVED ne sont pas modifiables)
        if (course.getStatus() == CourseStatus.PENDING || course.getStatus() == CourseStatus.ARCHIVED) {
            throw new RuntimeException("Impossible de modifier un cours en statut " + course.getStatus());
        }

        // ──── COURS PUBLIÉ → stocker en pending edit (JSON) ────────────
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            return savePendingEdit(course, dto);
        }

        // ──── COURS DRAFT / REJECTED → modification directe ───────────
        applyDtoToCourse(course, dto);

        // Gestion du statut après modification
        if (course.getStatus() == CourseStatus.REJECTED) {
            course.setStatus(CourseStatus.DRAFT);
            course.setRejectionReason(null);
        }
        // DRAFT reste DRAFT

        course = courseRepository.save(course);
        return toResponseDto(course);
    }

    /**
     * Applique les champs du DTO directement sur l'entité Course.
     * Utilisé pour DRAFT/REJECTED (modification immédiate) et lors de l'approbation d'un pending edit.
     */
    private void applyDtoToCourse(Course course, CreateCourseDto dto) {
        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setPrice(dto.getPrice());

        try {
            course.setLevel(CourseLevel.valueOf(dto.getLevel().toUpperCase()));
        } catch (Exception e) {
            // garder le niveau actuel si invalide
        }

        if (dto.getObjectives() != null) course.setObjectives(dto.getObjectives());
        if (dto.getRequirements() != null) course.setRequirements(dto.getRequirements());
        if (dto.getLanguage() != null) course.setLanguage(dto.getLanguage());
        if (dto.getDiscountPrice() != null) course.setDiscountPrice(dto.getDiscountPrice());
        if (dto.getDiscountEndsAt() != null) course.setDiscountEndsAt(dto.getDiscountEndsAt());

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
            course.setCategory(category);
        }
    }

    /**
     * Sauvegarde les modifications proposées en JSON dans le champ pending_edit.
     * Le cours reste PUBLISHED — les étudiants continuent de voir la version actuelle.
     */
    private CourseResponseDto savePendingEdit(Course course, CreateCourseDto dto) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        Map<String, Object> pending = new HashMap<>();
        pending.put("title", dto.getTitle());
        pending.put("description", dto.getDescription());
        pending.put("price", dto.getPrice());
        pending.put("level", dto.getLevel());
        pending.put("objectives", dto.getObjectives());
        pending.put("requirements", dto.getRequirements());
        pending.put("language", dto.getLanguage());
        pending.put("discountPrice", dto.getDiscountPrice());
        pending.put("discountEndsAt", dto.getDiscountEndsAt());
        pending.put("categoryId", dto.getCategoryId());

        try {
            course.setPendingEdit(mapper.writeValueAsString(pending));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde des modifications");
        }

        course.setHasPendingEdit(true);
        course.setEditRejectionReason(null);
        course = courseRepository.save(course);

        // Notification aux admins
        notificationService.send(
                course.getInstructor(),
                NotificationType.COURSE_APPROVED,
                "📝 Modification en attente",
                "Votre modification du cours \"" + course.getTitle() + "\" a été enregistrée et est en attente de validation par l'administration.",
                "/instructor/instructor-course"
        );

        return toResponseDto(course);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INSTRUCTOR — Supprimer un cours (seulement DRAFT)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void deleteCourse(Long courseId, User instructor) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        if (!course.getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Vous n'êtes pas le propriétaire de ce cours");
        }

        if (course.getStatus() != CourseStatus.DRAFT) {
            throw new RuntimeException("Seul un cours en brouillon peut être supprimé");
        }

        courseRepository.delete(course); // cascade supprime sections et leçons
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INSTRUCTOR — Soumettre un cours pour validation (DRAFT → PENDING)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public CourseResponseDto submitForReview(Long courseId, User instructor) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        if (!course.getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Vous n'êtes pas le propriétaire de ce cours");
        }

        // PENDING et ARCHIVED ne sont jamais soumettables
        if (course.getStatus() == CourseStatus.PENDING || course.getStatus() == CourseStatus.ARCHIVED) {
            throw new RuntimeException("Impossible de soumettre un cours en statut " + course.getStatus());
        }

        // PUBLISHED : les modifications ont déjà été sauvegardées via updateCourse (pendingEdit).
        // On retourne simplement le cours tel quel — le admin verra le pendingEdit dans sa liste.
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            return toResponseDto(course);
        }

        // ══ VALIDATION CONTENU OBLIGATOIRE ══
        if (course.getSections() == null || course.getSections().isEmpty()) {
            throw new RuntimeException("Le cours doit contenir au moins une section avant d'être soumis.");
        }
        for (var section : course.getSections()) {
            if (section.getLessons() == null || section.getLessons().isEmpty()) {
                throw new RuntimeException("La section \"" + section.getTitle() + "\" ne contient aucune leçon.");
            }
            // Au moins UNE leçon de la section doit avoir du contenu
            boolean sectionHasContent = section.getLessons().stream().anyMatch(lesson ->
                    (lesson.getVideoUrl() != null && !lesson.getVideoUrl().isBlank())
                    || (lesson.getPdfUrl() != null && !lesson.getPdfUrl().isBlank())
                    || (lesson.getArticleContent() != null && !lesson.getArticleContent().isBlank())
            );
            if (!sectionHasContent) {
                throw new RuntimeException(
                    "La section \"" + section.getTitle() + "\" doit contenir au moins une leçon avec du contenu (vidéo, PDF ou article)."
                );
            }
        }

        course.setStatus(CourseStatus.PENDING);
        course = courseRepository.save(course);
        return toResponseDto(course);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INSTRUCTOR — Mes cours
    // ═══════════════════════════════════════════════════════════════════════

    public List<CourseResponseDto> getMyCourses(User instructor) {
        return courseRepository.findByInstructorId(instructor.getId())
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INSTRUCTOR — Ajouter une section à un cours
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public SectionDto addSection(Long courseId, String title, User instructor) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        if (!course.getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Vous n'êtes pas le propriétaire de ce cours");
        }

        Section section = new Section();
        section.setTitle(title);
        section.setOrderIndex(course.getSections().size()); // position à la fin
        section.setCourse(course);

        section = sectionRepository.save(section);
        return toSectionDto(section);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INSTRUCTOR — Ajouter une leçon à une section
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public LessonDto addLesson(Long sectionId, String title, String lessonType, boolean isFree, User instructor) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section non trouvée"));

        if (!section.getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Vous n'êtes pas le propriétaire de ce cours");
        }

        Lesson lesson = new Lesson();
        lesson.setTitle(title);
        lesson.setOrderIndex(section.getLessons().size());
        lesson.setSection(section);
        lesson.setFree(isFree);

        // Set lesson type (VIDEO, TEXT, PDF — default VIDEO)
        try {
            lesson.setLessonType(com.elearning.ProjetPfe.entity.LessonType.valueOf(lessonType));
        } catch (Exception e) {
            lesson.setLessonType(com.elearning.ProjetPfe.entity.LessonType.VIDEO);
        }

        lesson = lessonRepository.save(lesson);
        return toLessonDto(lesson);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INSTRUCTOR — Modifier le titre / accès d’une leçon
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public LessonDto updateLesson(Long lessonId, Map<String, Object> body, User instructor) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Leçon non trouvée"));
        if (!lesson.getSection().getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Vous n'êtes pas le propriétaire de ce cours");
        }
        if (body.containsKey("title")) {
            String title = (String) body.get("title");
            if (title != null && !title.isBlank()) lesson.setTitle(title.trim());
        }
        if (body.containsKey("isFree")) {
            lesson.setFree(Boolean.parseBoolean(body.get("isFree").toString()));
        }
        lesson = lessonRepository.save(lesson);
        return toLessonDto(lesson);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INSTRUCTOR — Supprimer une leçon
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void deleteLesson(Long lessonId, User instructor) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Leçon non trouvée"));
        Course course = lesson.getSection().getCourse();
        if (!course.getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Vous n'êtes pas le propriétaire de ce cours");
        }
        // Supprimer les fichiers physiques si présents
        fileStorageService.deleteFile(lesson.getVideoUrl());
        fileStorageService.deleteFile(lesson.getPdfUrl());
        // Supprimer la progression des étudiants et les ressources attachées
        lessonProgressRepository.deleteByLessonId(lessonId);
        resourceRepository.deleteAllByLessonId(lessonId);
        // Supprimer la leçon elle-même
        lessonRepository.delete(lesson);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN — Voir les cours en attente de validation
    // ═══════════════════════════════════════════════════════════════════════

    public List<CourseResponseDto> getPendingCourses() {
        return courseRepository.findByStatus(CourseStatus.PENDING)
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    public List<CourseResponseDto> getPublishedCoursesForAdmin() {
        return courseRepository.findByStatus(CourseStatus.PUBLISHED)
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    public List<CourseResponseDto> getAllCoursesForAdmin() {
        // Les cours DRAFT sont privés (visibles seulement par l'instructor)
        // L'admin ne voit que PENDING, PUBLISHED, REJECTED, ARCHIVED
        return courseRepository.findByStatusNot(CourseStatus.DRAFT)
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN — Accepter ou rejeter un cours
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public CourseResponseDto reviewCourse(Long courseId, CourseReviewDto dto) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        if (course.getStatus() != CourseStatus.PENDING) {
            throw new RuntimeException("Ce cours n'est pas en attente de validation");
        }

        String action = dto.getAction().toUpperCase();

        if ("APPROVE".equals(action)) {
            course.setStatus(CourseStatus.PUBLISHED);
            course.setPublishedAt(LocalDateTime.now()); // ← enregistre la date de publication
            course.setRejectionReason(null);
            courseRepository.save(course);

            // Envoyer email de félicitations à l'instructor
            sendCourseApprovedEmail(course);

            // Notification interne
            notificationService.send(
                    course.getInstructor(),
                    NotificationType.COURSE_APPROVED,
                    "✅ Cours approuvé !",
                    "Votre cours \"" + course.getTitle() + "\" a été approuvé et est maintenant publié.",
                    "/instructor/courses"
            );

        } else if ("REJECT".equals(action)) {
            if (dto.getRejectionReason() == null || dto.getRejectionReason().isBlank()) {
                throw new RuntimeException("La raison du rejet est obligatoire");
            }
            course.setStatus(CourseStatus.REJECTED);
            course.setRejectionReason(dto.getRejectionReason());
            courseRepository.save(course);

            // Envoyer email de rejet à l'instructor
            sendCourseRejectedEmail(course);

            // Notification interne
            notificationService.send(
                    course.getInstructor(),
                    NotificationType.COURSE_REJECTED,
                    "❌ Cours rejeté",
                    "Votre cours \"" + course.getTitle() + "\" a été rejeté. Raison : " + dto.getRejectionReason(),
                    "/instructor/courses"
            );

        } else {
            throw new RuntimeException("Action invalide. Utilisez APPROVE ou REJECT");
        }

        return toResponseDto(course);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN — Approuver une modification en attente (pending edit)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public CourseResponseDto approvePendingEdit(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        if (!course.isHasPendingEdit() || course.getPendingEdit() == null) {
            throw new RuntimeException("Ce cours n'a pas de modification en attente");
        }

        // Parser le JSON et appliquer les modifications
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        try {
            Map<String, Object> pending = mapper.readValue(
                    course.getPendingEdit(), new TypeReference<Map<String, Object>>() {});

            if (pending.get("title") != null) course.setTitle((String) pending.get("title"));
            if (pending.get("description") != null) course.setDescription((String) pending.get("description"));
            if (pending.get("objectives") != null) course.setObjectives((String) pending.get("objectives"));
            if (pending.get("requirements") != null) course.setRequirements((String) pending.get("requirements"));
            if (pending.get("language") != null) course.setLanguage((String) pending.get("language"));
            if (pending.get("level") != null) {
                try {
                    course.setLevel(CourseLevel.valueOf(((String) pending.get("level")).toUpperCase()));
                } catch (Exception e) { /* garder le niveau actuel */ }
            }
            if (pending.get("price") != null) {
                course.setPrice(new java.math.BigDecimal(pending.get("price").toString()));
            }
            if (pending.get("discountPrice") != null) {
                course.setDiscountPrice(new java.math.BigDecimal(pending.get("discountPrice").toString()));
            }
            if (pending.get("discountEndsAt") != null) {
                course.setDiscountEndsAt(LocalDateTime.parse(pending.get("discountEndsAt").toString()));
            }
            if (pending.get("categoryId") != null) {
                Long catId = Long.valueOf(pending.get("categoryId").toString());
                Category category = categoryRepository.findById(catId)
                        .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
                course.setCategory(category);
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur de lecture des modifications en attente");
        }

        // Nettoyer le pending edit
        course.setPendingEdit(null);
        course.setHasPendingEdit(false);
        course.setEditRejectionReason(null);
        course = courseRepository.save(course);

        // Notification à l'instructor
        notificationService.send(
                course.getInstructor(),
                NotificationType.COURSE_APPROVED,
                "✅ Modification approuvée !",
                "Les modifications de votre cours \"" + course.getTitle() + "\" ont été approuvées et sont maintenant visibles.",
                "/instructor/instructor-course"
        );

        return toResponseDto(course);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN — Rejeter une modification en attente
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public CourseResponseDto rejectPendingEdit(Long courseId, String reason) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        if (!course.isHasPendingEdit() || course.getPendingEdit() == null) {
            throw new RuntimeException("Ce cours n'a pas de modification en attente");
        }

        course.setPendingEdit(null);
        course.setHasPendingEdit(false);
        course.setEditRejectionReason(reason);
        course = courseRepository.save(course);

        // Notification à l'instructor
        notificationService.send(
                course.getInstructor(),
                NotificationType.COURSE_REJECTED,
                "❌ Modification rejetée",
                "La modification de votre cours \"" + course.getTitle() + "\" a été rejetée." + (reason != null ? " Raison : " + reason : ""),
                "/instructor/instructor-course"
        );

        return toResponseDto(course);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN — Cours avec modifications en attente
    // ═══════════════════════════════════════════════════════════════════════

    public List<CourseResponseDto> getCoursesWithPendingEdits() {
        return courseRepository.findByHasPendingEdit(true)
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INSTRUCTOR — Reviews d'un de ses cours
    // ═══════════════════════════════════════════════════════════════════════

    public List<Map<String, Object>> getInstructorCourseReviews(Long courseId, User instructor) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        if (!course.getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Vous n'êtes pas le propriétaire de ce cours");
        }

        return reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(review -> {
                    Map<String, Object> reviewMap = new HashMap<>();
                    reviewMap.put("id", review.getId());
                    reviewMap.put("rating", review.getRating());
                    reviewMap.put("comment", review.getComment());
                    reviewMap.put("createdAt", review.getCreatedAt());
                    reviewMap.put("studentName", review.getStudent().getFullName());
                    return reviewMap;
                })
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PUBLIC — Cours publiés avec filtres optionnels
    // ═══════════════════════════════════════════════════════════════════════

    public List<CourseResponseDto> getPublishedCourses(String search, Long categoryId, String level) {
        List<Course> courses;

        if (search != null && !search.isBlank()) {
            // Recherche par mot-clé dans titre + description
            courses = courseRepository.searchPublished(search.trim());
        } else if (categoryId != null) {
            // Filtre par catégorie
            courses = courseRepository.findByCategoryIdAndStatus(categoryId, CourseStatus.PUBLISHED);
        } else {
            // Tous les cours publiés
            courses = courseRepository.findByStatus(CourseStatus.PUBLISHED);
        }

        // Filtre par niveau (si fourni)
        if (level != null && !level.isBlank()) {
            try {
                CourseLevel courseLevel = CourseLevel.valueOf(level.toUpperCase());
                courses = courses.stream()
                        .filter(c -> c.getLevel() == courseLevel)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // niveau invalide → ignorer le filtre
            }
        }

        return courses.stream()
                .map(this::toPublicDto)
                .collect(Collectors.toList());
    }

    /** Ancien getPublishedCourses() conservé pour compatibilité interne */
    public List<CourseResponseDto> getPublishedCourses() {
        return getPublishedCourses(null, null, null);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PUBLIC — Cours mis en avant (featured)
    // ═══════════════════════════════════════════════════════════════════════

    public List<CourseResponseDto> getFeaturedCourses() {
        return courseRepository.findByStatusAndFeaturedTrue(CourseStatus.PUBLISHED)
                .stream()
                .map(this::toPublicDto)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PUBLIC — Détail d'un cours (curriculum public : URLs masquées si payant)
    // ═══════════════════════════════════════════════════════════════════════

    public CourseResponseDto getPublicCourseDetail(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new RuntimeException("Cours non disponible");
        }
        return toPublicDto(course);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN — Supprimer n'importe quel cours
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void adminDeleteCourse(Long courseId) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        // 1) Supprimer les progressions de leçons (FK via lesson → section → course)
        lessonProgressRepository.deleteByCourseId(courseId);
        // 2) Supprimer les progressions de cours
        courseProgressRepository.deleteByCourseId(courseId);
        // 3) Supprimer les avis
        reviewRepository.deleteByCourseId(courseId);
        // 4) Supprimer les éléments de panier
        cartItemRepository.deleteByCourseId(courseId);
        // 5) Supprimer les favoris
        wishlistItemRepository.deleteByCourseId(courseId);
        // 6) Supprimer les inscriptions
        enrollmentRepository.deleteByCourseId(courseId);
        // 7) Supprimer le cours (sections + leçons + ressources cascadées)
        courseRepository.deleteById(courseId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN — Archiver n'importe quel cours
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public CourseResponseDto adminArchiveCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        course.setStatus(CourseStatus.ARCHIVED);
        return toResponseDto(courseRepository.save(course));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN — Modifier le contenu d'un cours (titre, description, niveau...)
    //  L'admin peut modifier n'importe quel cours sans changer son statut.
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public CourseResponseDto adminEditCourse(Long courseId, CreateCourseDto dto) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        if (dto.getTitle() != null && !dto.getTitle().isBlank())
            course.setTitle(dto.getTitle());
        if (dto.getDescription() != null)
            course.setDescription(dto.getDescription());
        if (dto.getLevel() != null) {
            try { course.setLevel(CourseLevel.valueOf(dto.getLevel().toUpperCase())); }
            catch (Exception ignored) {}
        }
        if (dto.getLanguage() != null) course.setLanguage(dto.getLanguage());
        if (dto.getObjectives() != null) course.setObjectives(dto.getObjectives());
        if (dto.getRequirements() != null) course.setRequirements(dto.getRequirements());
        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
            course.setCategory(category);
        }

        return toResponseDto(courseRepository.save(course));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN — Modifier le prix
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public CourseResponseDto adminUpdatePrice(Long courseId, java.math.BigDecimal price) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        course.setPrice(price);
        return toResponseDto(courseRepository.save(course));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN — Mettre en promotion
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public CourseResponseDto adminSetPromotion(Long courseId, java.math.BigDecimal discountPrice, LocalDateTime discountEndsAt) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        course.setDiscountPrice(discountPrice);
        course.setDiscountEndsAt(discountEndsAt);
        return toResponseDto(courseRepository.save(course));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN — Featured toggle (mettre en avant sur la home)
    //  Règles :
    //    - Seul un cours PUBLISHED peut être featured
    //    - Un cours ARCHIVED ne peut pas être featured
    //    - Optionnel : limite configurable (app.featured.max.courses)
    // ═══════════════════════════════════════════════════════════════════════

    @org.springframework.beans.factory.annotation.Value("${app.featured.max.courses:10}")
    private int maxFeaturedCourses;

    @Transactional
    public CourseResponseDto toggleFeatured(Long courseId, boolean featured) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        // Vérifier que le cours est publié
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new RuntimeException("Seul un cours publié peut être mis en avant. Statut actuel : " + course.getStatus());
        }

        // Si on active le featured, vérifier la limite
        if (featured) {
            long currentFeaturedCount = courseRepository.countByStatusAndFeaturedTrue(CourseStatus.PUBLISHED);
            if (!course.isFeatured() && currentFeaturedCount >= maxFeaturedCourses) {
                throw new RuntimeException(
                        "Nombre maximum de cours en avant atteint (" + maxFeaturedCourses + "). " +
                        "Désactivez un autre cours en avant d'abord.");
            }
        }

        course.setFeatured(featured);
        return toResponseDto(courseRepository.save(course));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INSTRUCTOR — Archiver un cours (PUBLISHED → ARCHIVED)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public CourseResponseDto archiveCourse(Long courseId, User instructor) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        if (!course.getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Vous n'êtes pas le propriétaire de ce cours");
        }

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new RuntimeException("Seul un cours publié peut être archivé");
        }

        course.setStatus(CourseStatus.ARCHIVED);
        course = courseRepository.save(course);
        return toResponseDto(course);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STUDENT — Voir le contenu d'un cours (seulement si payé)
    // ═══════════════════════════════════════════════════════════════════════

    public CourseResponseDto getCourseContent(Long courseId, User student) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        // Vérifier si l'étudiant a payé / est inscrit
        boolean enrolled = enrollmentRepository.findByStudentIdAndCourseIdAndPaymentStatus(
                student.getId(), courseId, PaymentStatus.PAID
        ).isPresent();

        if (!enrolled) {
            throw new RuntimeException("Vous devez acheter ce cours pour accéder au contenu");
        }

        // Un étudiant inscrit (enrollment PAID) garde toujours accès au contenu,
        // quel que soit le statut actuel du cours (publié, archivé, etc.).

        return toResponseDto(course); // version complète avec sections et leçons
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  COMMUN — Voir un cours par ID (pour instructor propriétaire ou admin)
    // ═══════════════════════════════════════════════════════════════════════

    public CourseResponseDto getCourseById(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        return toResponseDto(course);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  EMAILS
    // ═══════════════════════════════════════════════════════════════════════

    private void sendCourseApprovedEmail(Course course) {
        String to = course.getInstructor().getEmail();
        String subject = "✅ Votre cours a été approuvé !";
        String body = "Bonjour " + course.getInstructor().getFullName() + ",\n\n"
                + "Félicitations ! Votre cours \"" + course.getTitle() + "\" a été approuvé par l'administration.\n\n"
                + "Il est maintenant visible par tous les étudiants sur la plateforme.\n\n"
                + "Cordialement,\n"
                + "L'équipe E-Learning";

        try {
            emailService.sendEmail(to, subject, body);
        } catch (Exception e) {
            System.err.println("Erreur envoi email approbation: " + e.getMessage());
        }
    }

    private void sendCourseRejectedEmail(Course course) {
        String to = course.getInstructor().getEmail();
        String subject = "❌ Votre cours a été refusé";
        String body = "Bonjour " + course.getInstructor().getFullName() + ",\n\n"
                + "Votre cours \"" + course.getTitle() + "\" a été refusé par l'administration.\n\n"
                + "Raison du refus :\n" + course.getRejectionReason() + "\n\n"
                + "Vous pouvez modifier votre cours et le soumettre à nouveau.\n\n"
                + "Cordialement,\n"
                + "L'équipe E-Learning";

        try {
            emailService.sendEmail(to, subject, body);
        } catch (Exception e) {
            System.err.println("Erreur envoi email rejet: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  UPLOAD — Image de couverture
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public CourseResponseDto uploadCoverImage(Long courseId, MultipartFile file, User instructor) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        if (!course.getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Vous n'êtes pas le propriétaire de ce cours");
        }

        // Supprimer l'ancienne image si elle existe
        fileStorageService.deleteFile(course.getCoverImage());

        String path = fileStorageService.storeFile(file, "covers");
        course.setCoverImage(path);
        course = courseRepository.save(course);
        return toResponseDto(course);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  UPLOAD — Vidéo d'une leçon
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public LessonDto uploadLessonVideo(Long lessonId, MultipartFile file, User instructor) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Leçon non trouvée"));

        if (!lesson.getSection().getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Vous n'êtes pas le propriétaire de ce cours");
        }

        // Vérifier la taille
        fileStorageService.validateVideoSize(file);

        // Supprimer l'ancienne vidéo si elle existe
        fileStorageService.deleteFile(lesson.getVideoUrl());

        String path = fileStorageService.storeFile(file, "videos");
        lesson.setVideoUrl(path);
        lesson.setVideoSize(file.getSize());
        lesson = lessonRepository.save(lesson);
        return toLessonDto(lesson);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  UPLOAD — PDF d'une leçon
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public LessonDto uploadLessonPdf(Long lessonId, MultipartFile file, User instructor) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Leçon non trouvée"));

        if (!lesson.getSection().getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Vous n'êtes pas le propriétaire de ce cours");
        }

        fileStorageService.deleteFile(lesson.getPdfUrl());

        String path = fileStorageService.storeFile(file, "pdfs");
        lesson.setPdfUrl(path);
        lesson = lessonRepository.save(lesson);
        return toLessonDto(lesson);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SUPPRESSION du contenu d'une leçon (vidéo / PDF / article)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public LessonDto clearLessonVideo(Long lessonId, User instructor) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Leçon non trouvée"));
        if (!lesson.getSection().getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Vous n'êtes pas le propriétaire de ce cours");
        }
        fileStorageService.deleteFile(lesson.getVideoUrl());
        lesson.setVideoUrl(null);
        lesson.setVideoSize(null);
        lesson = lessonRepository.save(lesson);
        return toLessonDto(lesson);
    }

    @Transactional
    public LessonDto clearLessonPdf(Long lessonId, User instructor) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Leçon non trouvée"));
        if (!lesson.getSection().getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Vous n'êtes pas le propriétaire de ce cours");
        }
        fileStorageService.deleteFile(lesson.getPdfUrl());
        lesson.setPdfUrl(null);
        lesson = lessonRepository.save(lesson);
        return toLessonDto(lesson);
    }

    @Transactional
    public LessonDto clearLessonArticle(Long lessonId, User instructor) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Leçon non trouvée"));
        if (!lesson.getSection().getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Vous n'êtes pas le propriétaire de ce cours");
        }
        lesson.setArticleContent(null);
        lesson.setLessonType(com.elearning.ProjetPfe.entity.LessonType.VIDEO);
        lesson = lessonRepository.save(lesson);
        return toLessonDto(lesson);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CONVERSION Entity → DTO
    // ═══════════════════════════════════════════════════════════════════════

    // ─── MÉTHODE HELPER : remplit les champs communs ─────────────────────
    private void fillCommonFields(CourseResponseDto dto, Course course) {
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setDescription(course.getDescription());
        dto.setObjectives(course.getObjectives());
        dto.setRequirements(course.getRequirements());
        dto.setLanguage(course.getLanguage());
        dto.setPrice(course.getPrice());
        dto.setDiscountPrice(course.getDiscountPrice());
        dto.setDiscountEndsAt(course.getDiscountEndsAt());
        dto.setEffectivePrice(course.getEffectivePrice()); // prix avec promo appliquée
        dto.setOnSale(course.isOnSale());
        dto.setTotalDurationSeconds(course.getTotalDurationSeconds());
        dto.setCoverImage(course.getCoverImage());
        dto.setLevel(course.getLevel().name());
        dto.setStatus(course.getStatus().name());
        dto.setRejectionReason(course.getRejectionReason());
        dto.setFeatured(course.isFeatured());
        dto.setPublishedAt(course.getPublishedAt());
        dto.setInstructorId(course.getInstructor().getId());
        dto.setInstructorName(course.getInstructor().getFullName());
        dto.setCreatedAt(course.getCreatedAt());
        dto.setUpdatedAt(course.getUpdatedAt());

        // Catégorie (nullable)
        if (course.getCategory() != null) {
            dto.setCategoryId(course.getCategory().getId());
            dto.setCategoryName(course.getCategory().getName());
        }

        // Note moyenne et nombre d'avis (calculés depuis la table reviews)
        dto.setAverageRating(reviewRepository.calculateAverageRating(course.getId()));
        dto.setReviewCount(reviewRepository.countByCourseId(course.getId()));

        // Nombre d'étudiants inscrits (PAID uniquement)
        dto.setEnrollmentCount(enrollmentRepository.countByCourseIdAndPaymentStatus(
                course.getId(), PaymentStatus.PAID));

        // Versioning : modification en attente
        dto.setHasPendingEdit(course.isHasPendingEdit());
        dto.setEditRejectionReason(course.getEditRejectionReason());
        if (course.isHasPendingEdit() && course.getPendingEdit() != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                Map<String, Object> pendingData = mapper.readValue(
                        course.getPendingEdit(), new TypeReference<Map<String, Object>>() {});
                dto.setPendingEditData(pendingData);
            } catch (JsonProcessingException e) {
                // ignorer si le JSON est invalide
            }
        }
    }

    /** Convertit un Course en DTO complet (avec sections et leçons) */
    private CourseResponseDto toResponseDto(Course course) {
        CourseResponseDto dto = new CourseResponseDto();
        fillCommonFields(dto, course);

        // Inclure les sections avec leurs leçons
        if (course.getSections() != null) {
            dto.setSections(
                course.getSections().stream()
                    .map(this::toSectionDto)
                    .collect(Collectors.toList())
            );
        }
        return dto;
    }

    /**
     * DTO PUBLIC : même champs de base, mais les URLs de vidéo/PDF des leçons
     * non-gratuites sont masquées (null) pour les visiteurs non-inscrits.
     */
    private CourseResponseDto toPublicDto(Course course) {
        CourseResponseDto dto = new CourseResponseDto();
        fillCommonFields(dto, course);

        // Curriculum public : sections visibles mais contenu protégé
        if (course.getSections() != null) {
            dto.setSections(
                course.getSections().stream()
                    .map(section -> toSectionDtoPublic(section))
                    .collect(Collectors.toList())
            );
        }
        return dto;
    }

    /**
     * Section pour la page publique : les leçons non-gratuites n'ont pas leur URL.
     */
    private SectionDto toSectionDtoPublic(Section section) {
        SectionDto dto = new SectionDto();
        dto.setId(section.getId());
        dto.setTitle(section.getTitle());
        dto.setOrderIndex(section.getOrderIndex());
        if (section.getLessons() != null) {
            dto.setLessons(
                section.getLessons().stream()
                    .map(lesson -> {
                        LessonDto ld = toLessonDto(lesson);
                        // Masquer le contenu des leçons payantes
                        if (!lesson.isFree()) {
                            ld.setVideoUrl(null);
                            ld.setPdfUrl(null);
                        }
                        return ld;
                    })
                    .collect(Collectors.toList())
            );
        }
        return dto;
    }

    private SectionDto toSectionDto(Section section) {
        SectionDto dto = new SectionDto();
        dto.setId(section.getId());
        dto.setTitle(section.getTitle());
        dto.setOrderIndex(section.getOrderIndex());
        if (section.getLessons() != null) {
            dto.setLessons(
                section.getLessons().stream()
                    .map(this::toLessonDto)
                    .collect(Collectors.toList())
            );
        }
        return dto;
    }

    public LessonDto toLessonDto(Lesson lesson) {
        LessonDto dto = new LessonDto();
        dto.setId(lesson.getId());
        dto.setTitle(lesson.getTitle());
        dto.setDescription(lesson.getDescription());
        dto.setOrderIndex(lesson.getOrderIndex());
        dto.setLessonType(lesson.getLessonType() != null ? lesson.getLessonType().name() : "VIDEO");
        dto.setDurationSeconds(lesson.getDurationSeconds());
        dto.setFree(lesson.isFree());
        dto.setVideoUrl(lesson.getVideoUrl());
        dto.setVideoSize(lesson.getVideoSize());
        dto.setPdfUrl(lesson.getPdfUrl());
        dto.setArticleContent(lesson.getArticleContent());
        // Quiz associé
        quizRepository.findFirstByLessonId(lesson.getId()).ifPresent(q -> {
            dto.setHasQuiz(true);
            dto.setQuizId(q.getId());
            dto.setQuizTitle(q.getTitle());
        });
        return dto;
    }
    // ═══════════════════════════════════════════════════════════════════════
    //  INSTRUCTOR — Sauvegarder le contenu article d'une leçon
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public LessonDto saveArticleContent(Long lessonId, String content, User instructor) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Leçon introuvable"));
        if (!lesson.getSection().getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Accès non autorisé");
        }
        lesson.setArticleContent(content);
        lesson.setLessonType(com.elearning.ProjetPfe.entity.LessonType.TEXT);
        lesson = lessonRepository.save(lesson);
        return toLessonDto(lesson);
    }
}