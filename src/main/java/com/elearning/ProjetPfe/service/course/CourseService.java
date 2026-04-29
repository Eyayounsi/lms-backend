package com.elearning.ProjetPfe.service.course;

import com.elearning.ProjetPfe.service.communication.EmailService;
import com.elearning.ProjetPfe.service.communication.NotificationService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.multipart.MultipartFile;

import com.elearning.ProjetPfe.dto.course.CourseResponseDto;
import com.elearning.ProjetPfe.dto.engagement.CourseReviewDto;
import com.elearning.ProjetPfe.dto.course.CreateCourseDto;
import com.elearning.ProjetPfe.dto.course.LessonDto;
import com.elearning.ProjetPfe.dto.course.SectionDto;
import com.elearning.ProjetPfe.entity.course.Category;
import com.elearning.ProjetPfe.entity.course.Course;
import com.elearning.ProjetPfe.entity.course.CourseLevel;
import com.elearning.ProjetPfe.entity.course.CourseStatus;
import com.elearning.ProjetPfe.entity.course.Lesson;
import com.elearning.ProjetPfe.entity.communication.NotificationType;
import com.elearning.ProjetPfe.entity.payment.PaymentStatus;
import com.elearning.ProjetPfe.entity.course.Section;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.entity.auth.Role;
import com.elearning.ProjetPfe.repository.auth.UserRepository;
import com.elearning.ProjetPfe.repository.course.CategoryRepository;
import com.elearning.ProjetPfe.repository.payment.CartItemRepository;
import com.elearning.ProjetPfe.repository.learning.CourseProgressRepository;
import com.elearning.ProjetPfe.repository.course.CourseRepository;
import com.elearning.ProjetPfe.repository.payment.EnrollmentRepository;
import com.elearning.ProjetPfe.repository.learning.LessonProgressRepository;
import com.elearning.ProjetPfe.repository.course.LessonRepository;
import com.elearning.ProjetPfe.repository.course.ResourceRepository;
import com.elearning.ProjetPfe.repository.engagement.ReviewRepository;
import com.elearning.ProjetPfe.repository.course.SectionRepository;
import com.elearning.ProjetPfe.repository.payment.WishlistItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

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
    private UserRepository userRepository;

    @Autowired
    private com.elearning.ProjetPfe.repository.learning.QuizRepository quizRepository;

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
     *
     * Le JSON contient :
     *   - les métadonnées du cours (title, description, price, ...)
     *   - sectionsSnapshot : snapshot complet des sections/leçons ACTUELLES
     *     (avant que l'instructor ne modifie le contenu des leçons).
     *     Ce snapshot sert à restaurer l'ancienne version si l'admin rejette.
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

        // ── Snapshot de l'état actuel des sections/leçons ──────────────────
        // Sauvegarder UNIQUEMENT si aucun snapshot n'existe déjà (premier appel).
        // Si l'instructor modifie plusieurs fois avant approbation, on garde
        // le snapshot de la version originale publiée (pas d'écrasement).
        if (!course.isHasPendingEdit() || course.getPendingEdit() == null) {
            List<Map<String, Object>> sectionsSnap = buildSectionsSnapshot(course);
            pending.put("sectionsSnapshot", sectionsSnap);
        } else {
            // Conserver le snapshot existant pour ne pas perdre la version originale
            try {
                Map<String, Object> existing = mapper.readValue(
                        course.getPendingEdit(), new TypeReference<Map<String, Object>>() {});
                if (existing.containsKey("sectionsSnapshot")) {
                    pending.put("sectionsSnapshot", existing.get("sectionsSnapshot"));
                } else {
                    pending.put("sectionsSnapshot", buildSectionsSnapshot(course));
                }
            } catch (JsonProcessingException e) {
                pending.put("sectionsSnapshot", buildSectionsSnapshot(course));
            }
        }

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

    /**
     * Construit un snapshot sérialisable des sections et leçons actuelles du cours.
     * Utilisé pour pouvoir restaurer l'ancienne version si l'admin rejette la modification.
     */
    private List<Map<String, Object>> buildSectionsSnapshot(Course course) {
        List<Map<String, Object>> sectionsSnap = new java.util.ArrayList<>();
        if (course.getSections() == null) return sectionsSnap;

        for (Section section : course.getSections()) {
            Map<String, Object> sSnap = new HashMap<>();
            sSnap.put("id", section.getId());
            sSnap.put("title", section.getTitle());
            sSnap.put("orderIndex", section.getOrderIndex());

            List<Map<String, Object>> lessonsSnap = new java.util.ArrayList<>();
            if (section.getLessons() != null) {
                for (Lesson lesson : section.getLessons()) {
                    Map<String, Object> lSnap = new HashMap<>();
                    lSnap.put("id", lesson.getId());
                    lSnap.put("title", lesson.getTitle());
                    lSnap.put("description", lesson.getDescription());
                    lSnap.put("orderIndex", lesson.getOrderIndex());
                    lSnap.put("lessonType", lesson.getLessonType() != null ? lesson.getLessonType().name() : "VIDEO");
                    lSnap.put("free", lesson.isFree());
                    lSnap.put("videoUrl", lesson.getVideoUrl());
                    lSnap.put("videoSize", lesson.getVideoSize());
                    lSnap.put("pdfUrl", lesson.getPdfUrl());
                    lSnap.put("articleContent", lesson.getArticleContent());
                    lSnap.put("durationSeconds", lesson.getDurationSeconds());
                    lessonsSnap.add(lSnap);
                }
            }
            sSnap.put("lessons", lessonsSnap);
            sectionsSnap.add(sSnap);
        }
        return sectionsSnap;
    }

    /**
     * S'assure qu'un snapshot existe AVANT toute modification de leçon sur un cours publié.
     * Cette méthode est appelée par uploadLessonVideo, uploadLessonPdf, updateLesson, etc.
     * pour garantir que le snapshot capture l'état AVANT la modification.
     */
    @Transactional
    private void ensureSnapshotBeforeLessonModification(Course course) {
        // Seulement pour les cours publiés
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            return;
        }

        // Si un snapshot existe déjà, ne rien faire (on garde la version originale)
        if (course.isHasPendingEdit() && course.getPendingEdit() != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                Map<String, Object> existing = mapper.readValue(
                        course.getPendingEdit(), new TypeReference<Map<String, Object>>() {});
                if (existing.containsKey("sectionsSnapshot")) {
                    return; // Snapshot déjà présent
                }
            } catch (JsonProcessingException e) {
                // Continuer pour créer un nouveau snapshot
            }
        }

        // Créer un snapshot de l'état actuel (AVANT modification)
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        
        Map<String, Object> pending = new HashMap<>();
        // Copier les métadonnées existantes si elles existent
        if (course.getPendingEdit() != null) {
            try {
                Map<String, Object> existing = mapper.readValue(
                        course.getPendingEdit(), new TypeReference<Map<String, Object>>() {});
                pending.putAll(existing);
            } catch (JsonProcessingException e) {
                // Ignorer et créer un nouveau pending
            }
        }

        // Ajouter le snapshot des sections/leçons
        List<Map<String, Object>> sectionsSnap = buildSectionsSnapshot(course);
        pending.put("sectionsSnapshot", sectionsSnap);

        try {
            course.setPendingEdit(mapper.writeValueAsString(pending));
            course.setHasPendingEdit(true);
            courseRepository.save(course);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur lors de la création du snapshot");
        }
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
            lesson.setLessonType(com.elearning.ProjetPfe.entity.course.LessonType.valueOf(lessonType));
        } catch (Exception e) {
            lesson.setLessonType(com.elearning.ProjetPfe.entity.course.LessonType.VIDEO);
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

        // ── VERSIONING : créer snapshot AVANT modification si cours publié ──
        Course course = lesson.getSection().getCourse();
        ensureSnapshotBeforeLessonModification(course);

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

        if (dto.getAction() == null) {
            throw new RuntimeException("L'action (APPROVE ou REJECT) est obligatoire");
        }
        String action = dto.getAction().toUpperCase();

        switch (action) {
            case "APPROVE" -> {
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
            }

            case "REJECT" -> {
            String reason = (dto.getRejectionReason() != null && !dto.getRejectionReason().isBlank())
                ? dto.getRejectionReason().trim()
                : null;
            course.setStatus(CourseStatus.REJECTED);
            course.setRejectionReason(reason);
            courseRepository.save(course);

            // Envoyer email de rejet à l'instructor
            sendCourseRejectedEmail(course);

            // Notification interne
            String reasonLabel = (reason != null) ? reason : "Aucune raison spécifiée";
            notificationService.send(
                course.getInstructor(),
                NotificationType.COURSE_REJECTED,
                "❌ Cours rejeté",
                "Votre cours \"" + course.getTitle() + "\" a été rejeté. Raison : " + reasonLabel,
                "/instructor/courses"
            );
            }

            default -> throw new RuntimeException("Action invalide. Utilisez APPROVE ou REJECT");
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

            // Appliquer les modifications sur les sections/leçons si présentes
            if (pending.get("sections") != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> sectionsPending = (List<Map<String, Object>>) pending.get("sections");
                if (sectionsPending != null) {
                    for (Map<String, Object> sPending : sectionsPending) {
                        Long sId = toLong(sPending.get("id"));
                        Section section = sId != null ? sectionRepository.findById(sId).orElse(null) : null;
                        if (section == null) {
                            section = new Section();
                            section.setCourse(course);
                        }
                        if (sPending.get("title") != null) section.setTitle((String) sPending.get("title"));
                        if (sPending.get("orderIndex") != null) section.setOrderIndex(toInt(sPending.get("orderIndex")));
                        section = sectionRepository.save(section);

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> lessonsPending = (List<Map<String, Object>>) sPending.get("lessons");
                        if (lessonsPending != null) {
                            for (Map<String, Object> lPending : lessonsPending) {
                                Long lId = toLong(lPending.get("id"));
                                Lesson lesson = lId != null ? lessonRepository.findById(lId).orElse(null) : null;
                                if (lesson == null) {
                                    lesson = new Lesson();
                                    lesson.setSection(section);
                                }
                                if (lPending.get("title") != null) lesson.setTitle((String) lPending.get("title"));
                                if (lPending.get("description") != null) lesson.setDescription((String) lPending.get("description"));
                                if (lPending.get("orderIndex") != null) lesson.setOrderIndex(toInt(lPending.get("orderIndex")));
                                if (lPending.get("free") != null) lesson.setFree(Boolean.TRUE.equals(lPending.get("free")));
                                if (lPending.get("videoUrl") != null) lesson.setVideoUrl((String) lPending.get("videoUrl"));
                                if (lPending.get("videoSize") != null) lesson.setVideoSize(toLong(lPending.get("videoSize")));
                                if (lPending.get("pdfUrl") != null) lesson.setPdfUrl((String) lPending.get("pdfUrl"));
                                if (lPending.get("articleContent") != null) lesson.setArticleContent((String) lPending.get("articleContent"));
                                if (lPending.get("durationSeconds") != null) lesson.setDurationSeconds(toLong(lPending.get("durationSeconds")));
                                String lt = (String) lPending.get("lessonType");
                                try {
                                    if (lt != null) lesson.setLessonType(com.elearning.ProjetPfe.entity.course.LessonType.valueOf(lt));
                                } catch (Exception e) {
                                    lesson.setLessonType(com.elearning.ProjetPfe.entity.course.LessonType.VIDEO);
                                }
                                lessonRepository.save(lesson);
                            }
                        }
                    }
                }
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

        // ── Restaurer le snapshot des sections/leçons ──────────────────────
        // Si l'instructor a modifié le contenu des leçons (PDF, vidéo, article)
        // pendant la période de pending, on restaure l'ancienne version.
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            Map<String, Object> pending = mapper.readValue(
                    course.getPendingEdit(), new TypeReference<Map<String, Object>>() {});

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sectionsSnapshot =
                    (List<Map<String, Object>>) pending.get("sectionsSnapshot");

            if (sectionsSnapshot != null && !sectionsSnapshot.isEmpty()) {
                restoreSectionsFromSnapshot(course, sectionsSnapshot);
            }
        } catch (Exception e) {
            log.warn("[REJECT_EDIT] Impossible de restaurer le snapshot du cours {} : {}", courseId, e.getMessage());
            // Ne pas bloquer le rejet si la restauration échoue
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

    /**
     * Restaure les sections et leçons d'un cours depuis un snapshot JSON.
     *
     * Stratégie :
     *   1. Pour chaque section du snapshot → mettre à jour titre/ordre si elle existe encore
     *   2. Pour chaque leçon du snapshot → restaurer tous les champs de contenu
     *   3. Les sections/leçons ajoutées par l'instructor pendant le pending
     *      (qui n'existent pas dans le snapshot) sont supprimées.
     *   4. Les sections/leçons du snapshot qui n'existent plus sont recréées.
     */
    @Transactional
    private void restoreSectionsFromSnapshot(Course course,
                                              List<Map<String, Object>> sectionsSnapshot) {
        // IDs des sections et leçons présents dans le snapshot
        java.util.Set<Long> snapshotSectionIds = new java.util.HashSet<>();
        java.util.Set<Long> snapshotLessonIds  = new java.util.HashSet<>();

        for (Map<String, Object> sSnap : sectionsSnapshot) {
            Long sId = toLong(sSnap.get("id"));
            if (sId != null) snapshotSectionIds.add(sId);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> lSnaps = (List<Map<String, Object>>) sSnap.get("lessons");
            if (lSnaps != null) {
                for (Map<String, Object> lSnap : lSnaps) {
                    Long lId = toLong(lSnap.get("id"));
                    if (lId != null) snapshotLessonIds.add(lId);
                }
            }
        }

        // ── Supprimer les sections ajoutées pendant le pending ─────────────
        List<Section> currentSections = new java.util.ArrayList<>(course.getSections());
        for (Section section : currentSections) {
            if (!snapshotSectionIds.contains(section.getId())) {
                // Section créée pendant le pending → supprimer ses leçons d'abord
                for (Lesson lesson : new java.util.ArrayList<>(section.getLessons())) {
                    lessonProgressRepository.deleteByLessonId(lesson.getId());
                    resourceRepository.deleteAllByLessonId(lesson.getId());
                }
                course.getSections().remove(section);
                sectionRepository.delete(section);
            }
        }

        // ── Restaurer chaque section du snapshot ───────────────────────────
        for (Map<String, Object> sSnap : sectionsSnapshot) {
            Long sId = toLong(sSnap.get("id"));
            String sTitle = (String) sSnap.get("title");
            int sOrder = toInt(sSnap.get("orderIndex"));

            Section section = sectionRepository.findById(sId).orElse(null);
            if (section == null) {
                // Section supprimée pendant le pending → recréer
                section = new Section();
                section.setCourse(course);
            }
            section.setTitle(sTitle);
            section.setOrderIndex(sOrder);
            section = sectionRepository.save(section);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> lSnaps = (List<Map<String, Object>>) sSnap.get("lessons");
            if (lSnaps == null) lSnaps = java.util.Collections.emptyList();

            // Supprimer les leçons ajoutées pendant le pending dans cette section
            for (Lesson lesson : new java.util.ArrayList<>(section.getLessons())) {
                if (!snapshotLessonIds.contains(lesson.getId())) {
                    lessonProgressRepository.deleteByLessonId(lesson.getId());
                    resourceRepository.deleteAllByLessonId(lesson.getId());
                    section.getLessons().remove(lesson);
                    lessonRepository.delete(lesson);
                }
            }

            // Restaurer chaque leçon du snapshot
            for (Map<String, Object> lSnap : lSnaps) {
                Long lId = toLong(lSnap.get("id"));
                Lesson lesson = lId != null ? lessonRepository.findById(lId).orElse(null) : null;
                if (lesson == null) {
                    lesson = new Lesson();
                    lesson.setSection(section);
                }
                lesson.setTitle((String) lSnap.get("title"));
                lesson.setDescription((String) lSnap.get("description"));
                lesson.setOrderIndex(toInt(lSnap.get("orderIndex")));
                lesson.setFree(Boolean.TRUE.equals(lSnap.get("free")));
                lesson.setVideoUrl((String) lSnap.get("videoUrl"));
                lesson.setVideoSize(toLong(lSnap.get("videoSize")));
                lesson.setPdfUrl((String) lSnap.get("pdfUrl"));
                lesson.setArticleContent((String) lSnap.get("articleContent"));
                lesson.setDurationSeconds(toLong(lSnap.get("durationSeconds")));
                String lt = (String) lSnap.get("lessonType");
                try {
                    lesson.setLessonType(com.elearning.ProjetPfe.entity.course.LessonType.valueOf(lt));
                } catch (Exception e) {
                    lesson.setLessonType(com.elearning.ProjetPfe.entity.course.LessonType.VIDEO);
                }
                lessonRepository.save(lesson);
            }
        }
    }

    /** Convertit un Object en Long de façon sûre (Integer, Long, String). */
    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long) return (Long) val;
        if (val instanceof Integer) return ((Integer) val).longValue();
        try { return Long.valueOf(val.toString()); } catch (Exception e) { return null; }
    }

    /** Convertit un Object en int de façon sûre. */
    private int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Long) return ((Long) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return 0; }
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

    @Cacheable(
            cacheNames = "publishedCourses",
            key = "(#search == null ? '' : #search.trim().toLowerCase()) + '|' + (#categoryId == null ? '' : #categoryId) + '|' + (#level == null ? '' : #level.trim().toUpperCase())"
    )
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

    @Cacheable(cacheNames = "featuredCourses")
    public List<CourseResponseDto> getFeaturedCourses() {
        return courseRepository.findByStatusAndFeaturedTrue(CourseStatus.PUBLISHED)
                .stream()
                .map(this::toPublicDto)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PUBLIC — Détail d'un cours (curriculum public : URLs masquées si payant)
    // ═══════════════════════════════════════════════════════════════════════

    @Cacheable(cacheNames = "publicCourseDetail", key = "#courseId")
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

        // Si le cours a une modification en attente, la rejeter automatiquement
        // (logique métier: un cours archivé n'a plus besoin de modifications en attente)
        if (course.isHasPendingEdit()) {
            log.info("[ARCHIVE] Rejet automatique de la modification en attente du cours {} avant archivage", courseId);
            course.setPendingEdit(null);
            course.setHasPendingEdit(false);
            course.setEditRejectionReason("Cours archivé par l'administration");
        }

        course.setStatus(CourseStatus.ARCHIVED);
        try {
            course = courseRepository.save(course);
        } catch (Exception e) {
            log.error("[ARCHIVE] Erreur sauvegarde cours {} au statut ARCHIVED: {}", courseId, e.getMessage());
            throw new RuntimeException("Erreur lors de l'archivage du cours: " + e.getMessage());
        }

        // Notifier l'instructor (asynchrone, ne doit pas bloquer l'archivage)
        if (course.getInstructor() != null) {
            try {
                notificationService.send(
                    course.getInstructor(),
                    NotificationType.COURSE_ARCHIVED_BY_ADMIN,
                    "📦 Cours archivé par l'administration",
                    "Votre cours \"" + course.getTitle() + "\" a été archivé par l'administration.",
                    "/instructor/instructor-course"
                );
            } catch (Exception e) {
                log.warn("[ARCHIVE] Erreur notification instructor cours {}: {}", courseId, e.getMessage());
                // Ne pas échouer l'archivage si la notification plante
            }
        }

        return toResponseDto(course);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN — Désarchiver un cours (ARCHIVED → PUBLISHED)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public CourseResponseDto adminUnarchiveCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        if (course.getStatus() != CourseStatus.ARCHIVED) {
            throw new RuntimeException("Ce cours n'est pas archivé");
        }
        
        // Si le cours a une modification en attente (rare mais possible), la rejeter automatiquement
        if (course.isHasPendingEdit()) {
            log.info("[UNARCHIVE] Rejet automatique de la modification en attente du cours {} avant désarchivage", courseId);
            course.setPendingEdit(null);
            course.setHasPendingEdit(false);
            course.setEditRejectionReason("Cours désarchivé par l'administration");
        }
        
        course.setStatus(CourseStatus.PUBLISHED);
        try {
            course = courseRepository.save(course);
        } catch (Exception e) {
            log.error("[UNARCHIVE] Erreur sauvegarde cours {} au statut PUBLISHED: {}", courseId, e.getMessage());
            throw new RuntimeException("Erreur lors du désarchivage du cours: " + e.getMessage());
        }

        // Notifier l'instructor (asynchrone, ne doit pas bloquer le désarchivage)
        if (course.getInstructor() != null) {
            try {
                notificationService.send(
                    course.getInstructor(),
                    NotificationType.COURSE_UNARCHIVED_BY_ADMIN,
                    "📤 Cours désarchivé par l'administration",
                    "Votre cours \"" + course.getTitle() + "\" a été désarchivé par l'administration.",
                    "/instructor/instructor-course"
                );
            } catch (Exception e) {
                log.warn("[UNARCHIVE] Erreur notification instructor cours {}: {}", courseId, e.getMessage());
                // Ne pas échouer le désarchivage si la notification plante
            }
        }

        return toResponseDto(course);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN — Liste des cours archivés
    // ═══════════════════════════════════════════════════════════════════════

    public List<CourseResponseDto> getArchivedCoursesForAdmin() {
        return courseRepository.findByStatus(CourseStatus.ARCHIVED)
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
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

        java.math.BigDecimal previousDiscountPrice = course.getDiscountPrice();
        LocalDateTime previousDiscountEndsAt = course.getDiscountEndsAt();

        course.setDiscountPrice(discountPrice);
        course.setDiscountEndsAt(discountEndsAt);
        course = courseRepository.save(course);

        boolean promotionChanged =
            (previousDiscountPrice == null && discountPrice != null)
            || (previousDiscountPrice != null && !previousDiscountPrice.equals(discountPrice))
            || (previousDiscountEndsAt == null && discountEndsAt != null)
            || (previousDiscountEndsAt != null && !previousDiscountEndsAt.equals(discountEndsAt));

        if (promotionChanged && course.getInstructor() != null) {
            String message;
            if (discountPrice != null && discountPrice.compareTo(course.getPrice()) < 0) {
            String endsAtLabel = (discountEndsAt != null)
                ? " jusqu'au " + discountEndsAt.toLocalDate()
                : " sans date de fin";
            message = "Une promotion a été appliquée sur votre cours \""
                + course.getTitle()
                + "\" ("
                + discountPrice
                + " €"
                + endsAtLabel
                + ").";
            } else {
            message = "La promotion de votre cours \"" + course.getTitle() + "\" a été retirée ou mise à jour.";
            }

            notificationService.send(
                course.getInstructor(),
                NotificationType.COURSE_PROMOTION_UPDATED,
                "🏷️ Promotion mise à jour",
                message,
                "/instructor/instructor-course"
            );
        }

        return toResponseDto(course);
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

        if (course.isHasPendingEdit()) {
            throw new RuntimeException("Impossible d'archiver un cours avec une modification en attente. Attendez la décision de l'administration.");
        }

        course.setStatus(CourseStatus.ARCHIVED);
        course = courseRepository.save(course);

        notifyAdminsForInstructorCourseStatusChange(
            NotificationType.COURSE_ARCHIVED_BY_INSTRUCTOR,
            "📦 Cours archivé par un instructeur",
            "L'instructeur " + instructor.getFullName() + " a archivé le cours \"" + course.getTitle() + "\".",
            "/admin/admin-courses"
        );

        return toResponseDto(course);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INSTRUCTOR — Désarchiver un cours (ARCHIVED → PUBLISHED)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public CourseResponseDto unarchiveCourse(Long courseId, User instructor) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        if (!course.getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Vous n'êtes pas le propriétaire de ce cours");
        }

        if (course.getStatus() != CourseStatus.ARCHIVED) {
            throw new RuntimeException("Seul un cours archivé peut être désarchivé");
        }

        if (course.isHasPendingEdit()) {
            throw new RuntimeException("Impossible de désarchiver un cours avec une modification en attente.");
        }

        course.setStatus(CourseStatus.PUBLISHED);
        course = courseRepository.save(course);

        notifyAdminsForInstructorCourseStatusChange(
                NotificationType.COURSE_UNARCHIVED_BY_INSTRUCTOR,
                "📤 Cours désarchivé par un instructeur",
                "L'instructeur " + instructor.getFullName() + " a désarchivé le cours \"" + course.getTitle() + "\".",
                "/admin/admin-courses"
        );

        return toResponseDto(course);
    }

    private void notifyAdminsForInstructorCourseStatusChange(
            NotificationType type,
            String title,
            String message,
            String link
    ) {
        List<User> admins = userRepository.findByRoleIn(List.of(Role.ADMIN, Role.SUPERADMIN));
        for (User admin : admins) {
            notificationService.send(admin, type, title, message, link);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STUDENT — Voir le contenu d'un cours (seulement si payé)
    // ═══════════════════════════════════════════════════════════════════════

    public CourseResponseDto getCourseContent(Long courseId, User student) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        log.info("[STUDENT_VIEW] Course ID={}, hasPendingEdit={}, status={}", 
                 courseId, course.isHasPendingEdit(), course.getStatus());

        // Vérifier si l'étudiant a payé / est inscrit
        boolean enrolled = enrollmentRepository.findByStudentIdAndCourseIdAndPaymentStatus(
                student.getId(), courseId, PaymentStatus.PAID
        ).isPresent();

        if (!enrolled) {
            throw new RuntimeException("Vous devez acheter ce cours pour accéder au contenu");
        }

        // Un étudiant inscrit (enrollment PAID) garde toujours accès au contenu,
        // quel que soit le statut actuel du cours (publié, archivé, etc.).
        //
        // IMPORTANT — versioning :
        //   • Si hasPendingEdit=true  → l'admin n'a pas encore statué.
        //     L'étudiant voit la version PUBLIÉE actuelle (les champs du cours
        //     n'ont PAS encore été modifiés — le JSON est dans pendingEdit).
        //   • Si l'admin APPROUVE    → approvePendingEdit() applique le JSON
        //     sur l'entité Course ; l'étudiant verra la nouvelle version au
        //     prochain appel.
        //   • Si l'admin REJETTE     → rejectPendingEdit() efface le JSON ;
        //     l'étudiant continue de voir la version actuelle inchangée.
        //
        // Dans tous les cas, toStudentDto() masque les métadonnées de
        // versioning (pendingEditData, hasPendingEdit, editRejectionReason)
        // qui sont réservées à l'admin et à l'instructor.
        //
        // On charge les sections et leçons DIRECTEMENT depuis les repositories
        // pour garantir des données fraîches (évite tout cache Hibernate L1/L2).

        return toStudentDtoFresh(course);
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
            log.warn("[EMAIL] Erreur envoi email approbation cours {} : {}", course.getId(), e.getMessage());
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
            log.warn("[EMAIL] Erreur envoi email rejet cours {} : {}", course.getId(), e.getMessage());
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

        // Supprimer l'ancienne image uploadée si elle existe
        String prev = course.getCoverImage();
        if (prev != null && !prev.startsWith("preset:")) {
            fileStorageService.deleteFile(prev);
        }

        String path = fileStorageService.storeFile(file, "covers");
        path = normalizeCoverImagePath(path);  // Normaliser même la sortie du service
        course.setCoverImage(path);
        course = courseRepository.save(course);
        return toResponseDto(course);
    }

    @Transactional
    public CourseResponseDto setPresetCoverImage(Long courseId, String imageName, User instructor) {
        // Validation plus souple pour les noms de fichiers (autorise points, tirets, underscores)
        if (imageName == null || !imageName.matches("^[a-zA-Z0-9_\\-\\.]+\\.(jpg|jpeg|png|webp|svg)$")) {
            throw new RuntimeException("Nom d'image invalide ou format non supporté : " + imageName);
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        if (!course.getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Vous n'êtes pas le propriétaire de ce cours");
        }

        // Supprimer l'ancienne image uploadée si elle existait
        String prev = course.getCoverImage();
        if (prev != null && !prev.startsWith("preset:")) {
            fileStorageService.deleteFile(prev);
        }

        course.setCoverImage("preset:" + imageName);
        course = courseRepository.save(course);
        return toResponseDto(course);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PRIVATE UTILITY — Normaliser les chemins d'images
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Normalise les chemins d'images pour garantir le format /uploads/...
     * Corrige les formats legacy :
     * - covers/... ou uploads/...
     * - chemins Windows absolus (ex: C:/.../uploads/covers/x.jpg)
     * - guillemets/backslashes
     *
     * @param path le chemin à normaliser (peut être null)
     * @return le chemin normalisé, ou null si invalide
     */
    private String normalizeCoverImagePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        String normalized = path.trim();

        // Retirer les guillemets parasites puis uniformiser les séparateurs
        normalized = normalized.replaceAll("^['\"]+|['\"]+$", "").trim();
        normalized = normalized.replace("\\", "/").replaceAll("/+", "/");
        if (normalized.isEmpty()) {
            return null;
        }

        String lower = normalized.toLowerCase();

        // Ignorer les images prédéfinies et URLs absolues
        if (lower.startsWith("preset:") || lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("data:")) {
            return normalized;
        }

        // Extraire la partie /uploads/... depuis un chemin absolu legacy
        int uploadsSlashIndex = lower.indexOf("/uploads/");
        if (uploadsSlashIndex > 0) {
            normalized = normalized.substring(uploadsSlashIndex);
            lower = normalized.toLowerCase();
        } else {
            int uploadsIndex = lower.indexOf("uploads/");
            if (uploadsIndex > 0) {
                normalized = "/" + normalized.substring(uploadsIndex);
                lower = normalized.toLowerCase();
            }
        }

        // Normaliser les préfixes legacy vers /uploads/...
        if (lower.startsWith("covers/") || lower.startsWith("videos/") || lower.startsWith("pdfs/") || lower.startsWith("avatars/")) {
            normalized = "/uploads/" + normalized;
            lower = normalized.toLowerCase();
        } else if (lower.startsWith("uploads/")) {
            normalized = "/" + normalized;
            lower = normalized.toLowerCase();
        }

        // Standardiser la casse du préfixe /uploads/
        if (lower.startsWith("/uploads/")) {
            normalized = "/uploads/" + normalized.substring(9);
        }

        // Fallback final pour garder un chemin relatif serveur valide
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        return normalized.isBlank() ? null : normalized;
    }

    /**
     * Retourne une image de couverture à exposer dans le DTO.
     */
    private String resolveCoverForDto(Course course) {
        String normalized = normalizeCoverImagePath(course.getCoverImage());
        if (normalized != null && !normalized.isBlank()) {
            return normalized;
        }
        return "preset:" + getDefaultCoursePresetFileName(course.getId());
    }

    /**
     * Choisit un preset déterministe pour éviter les cours sans image.
     */
    private String getDefaultCoursePresetFileName(Long courseId) {
        long safeId = (courseId == null) ? 1L : Math.abs(courseId);
        int index = (int) ((safeId % 9L) + 1L);
        return "course-img" + index + ".jpg";
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

        // ── VERSIONING : créer snapshot AVANT modification si cours publié ──
        Course course = lesson.getSection().getCourse();
        ensureSnapshotBeforeLessonModification(course);

        // Vérifier la taille
        fileStorageService.validateVideoSize(file);

        // Supprimer l'ancienne vidéo si elle existe
        fileStorageService.deleteFile(lesson.getVideoUrl());

        String path = fileStorageService.storeFile(file, "videos");
        lesson.setVideoUrl(path);
        lesson.setVideoSize(file.getSize());
        // Synchroniser le type : un upload vidéo rend la leçon de type VIDEO
        lesson.setLessonType(com.elearning.ProjetPfe.entity.course.LessonType.VIDEO);
        // Supprimer l'ancien contenu (PDF et article) pour éviter les conflits
        lesson.setPdfUrl(null);
        lesson.setArticleContent(null);
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

        // ── VALIDATION : Vérifier que le fichier est bien un PDF ──
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new RuntimeException("Le fichier doit être au format PDF (.pdf)");
        }
        
        if (contentType != null && !contentType.equals("application/pdf")) {
            throw new RuntimeException("Le type de fichier doit être application/pdf");
        }

        // ── VERSIONING : créer snapshot AVANT modification si cours publié ──
        Course course = lesson.getSection().getCourse();
        ensureSnapshotBeforeLessonModification(course);

        fileStorageService.deleteFile(lesson.getPdfUrl());

        String path = fileStorageService.storeFile(file, "pdfs");
        lesson.setPdfUrl(path);
        // Synchroniser le type : un upload PDF rend la leçon de type PDF
        lesson.setLessonType(com.elearning.ProjetPfe.entity.course.LessonType.PDF);
        // Supprimer l'ancien contenu (vidéo et article) pour éviter les conflits
        lesson.setVideoUrl(null);
        lesson.setVideoSize(null);
        lesson.setArticleContent(null);
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

        // ── VERSIONING : créer snapshot AVANT modification si cours publié ──
        Course course = lesson.getSection().getCourse();
        ensureSnapshotBeforeLessonModification(course);

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

        // ── VERSIONING : créer snapshot AVANT modification si cours publié ──
        Course course = lesson.getSection().getCourse();
        ensureSnapshotBeforeLessonModification(course);

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

        // ── VERSIONING : créer snapshot AVANT modification si cours publié ──
        Course course = lesson.getSection().getCourse();
        ensureSnapshotBeforeLessonModification(course);

        lesson.setArticleContent(null);
        lesson.setLessonType(com.elearning.ProjetPfe.entity.course.LessonType.VIDEO);
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
        dto.setCoverImage(resolveCoverForDto(course));
        dto.setLevel(course.getLevel().name());
        dto.setStatus(course.getStatus().name());
        dto.setRejectionReason(course.getRejectionReason());
        dto.setFeatured(course.isFeatured());
        dto.setPublishedAt(course.getPublishedAt());
        
        // Vérifier que l'instructor existe (sécurité)
        if (course.getInstructor() != null) {
            dto.setInstructorId(course.getInstructor().getId());
            dto.setInstructorName(course.getInstructor().getFullName());
        }
        
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
     * DTO ÉTUDIANT : contenu complet du cours (sections + leçons avec URLs),
     * mais sans les métadonnées de versioning réservées à l'admin/instructor.
     *
     * Règle de versioning :
     *   - Si hasPendingEdit=true  → les champs du cours sont la version PUBLIÉE
     *     actuelle (le JSON pendingEdit n'est pas encore appliqué).
     *     L'étudiant voit donc la bonne version en attente de décision admin.
     *   - Si l'admin APPROUVE     → approvePendingEdit() applique le JSON sur
     *     l'entité ; l'étudiant verra la nouvelle version au prochain appel.
     *   - Si l'admin REJETTE      → rejectPendingEdit() efface le JSON ;
     *     l'étudiant continue de voir la version actuelle inchangée.
     *
     * On masque hasPendingEdit, pendingEditData et editRejectionReason car
     * ce sont des données internes de workflow non destinées à l'étudiant.
     */
    private CourseResponseDto toStudentDto(Course course) {
        CourseResponseDto dto = new CourseResponseDto();
        fillCommonFields(dto, course);

        // Masquer les métadonnées de versioning — réservées à l'admin/instructor
        dto.setHasPendingEdit(false);
        dto.setPendingEditData(null);
        dto.setEditRejectionReason(null);

        // Inclure les sections avec leurs leçons (contenu complet, URLs incluses)
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
     * Variante de toStudentDto qui charge les sections et leçons DIRECTEMENT
     * depuis les repositories (requêtes SQL fraîches) pour garantir que
     * l'étudiant voit toujours la version en base, sans aucun cache Hibernate.
     *
     * Utilisé exclusivement par getCourseContent() pour éviter que le cache
     * de premier niveau Hibernate (L1) ou les collections lazy déjà initialisées
     * ne retournent une version stale après une modification approuvée par l'admin.
     */
    private CourseResponseDto toStudentDtoFresh(Course course) {
        CourseResponseDto dto = new CourseResponseDto();
        fillCommonFields(dto, course);

        // Masquer les métadonnées de versioning — réservées à l'admin/instructor
        dto.setHasPendingEdit(false);
        dto.setPendingEditData(null);
        dto.setEditRejectionReason(null);

        // Charger les sections directement depuis la DB (requête fraîche)
        List<Section> freshSections = sectionRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());
        log.info("[STUDENT_VIEW_FRESH] Course ID={}, sections count={}", course.getId(), freshSections.size());
        
        dto.setSections(
            freshSections.stream()
                .map(section -> {
                    SectionDto sDto = new SectionDto();
                    sDto.setId(section.getId());
                    sDto.setTitle(section.getTitle());
                    sDto.setOrderIndex(section.getOrderIndex());
                    // Charger les leçons directement depuis la DB (requête fraîche)
                    List<Lesson> freshLessons = lessonRepository.findBySectionIdOrderByOrderIndexAsc(section.getId());
                    log.info("[STUDENT_VIEW_FRESH] Section ID={}, lessons count={}", section.getId(), freshLessons.size());
                    
                    sDto.setLessons(
                        freshLessons.stream()
                            .map(lesson -> {
                                // ── AUTO-FIX : Corriger les incohérences de lessonType ──
                                boolean needsFix = false;
                                com.elearning.ProjetPfe.entity.course.LessonType correctedType = lesson.getLessonType();
                                
                                // Si PDF existe mais type != PDF
                                if (lesson.getPdfUrl() != null && !lesson.getPdfUrl().isBlank() 
                                    && lesson.getLessonType() != com.elearning.ProjetPfe.entity.course.LessonType.PDF) {
                                    correctedType = com.elearning.ProjetPfe.entity.course.LessonType.PDF;
                                    needsFix = true;
                                    log.warn("[AUTO-FIX] Lesson ID={} has PDF but type={}, correcting to PDF", 
                                             lesson.getId(), lesson.getLessonType());
                                }
                                // Si VIDEO existe mais type != VIDEO
                                else if (lesson.getVideoUrl() != null && !lesson.getVideoUrl().isBlank() 
                                         && lesson.getLessonType() != com.elearning.ProjetPfe.entity.course.LessonType.VIDEO) {
                                    correctedType = com.elearning.ProjetPfe.entity.course.LessonType.VIDEO;
                                    needsFix = true;
                                    log.warn("[AUTO-FIX] Lesson ID={} has VIDEO but type={}, correcting to VIDEO", 
                                             lesson.getId(), lesson.getLessonType());
                                }
                                // Si ARTICLE existe mais type != TEXT
                                else if (lesson.getArticleContent() != null && !lesson.getArticleContent().isBlank() 
                                         && lesson.getLessonType() != com.elearning.ProjetPfe.entity.course.LessonType.TEXT) {
                                    correctedType = com.elearning.ProjetPfe.entity.course.LessonType.TEXT;
                                    needsFix = true;
                                    log.warn("[AUTO-FIX] Lesson ID={} has ARTICLE but type={}, correcting to TEXT", 
                                             lesson.getId(), lesson.getLessonType());
                                }
                                
                                // Appliquer la correction en DB si nécessaire
                                if (needsFix) {
                                    lesson.setLessonType(correctedType);
                                    // Supprimer les contenus conflictuels
                                    if (correctedType == com.elearning.ProjetPfe.entity.course.LessonType.PDF) {
                                        lesson.setVideoUrl(null);
                                        lesson.setVideoSize(null);
                                        lesson.setArticleContent(null);
                                    } else if (correctedType == com.elearning.ProjetPfe.entity.course.LessonType.VIDEO) {
                                        lesson.setPdfUrl(null);
                                        lesson.setArticleContent(null);
                                    } else if (correctedType == com.elearning.ProjetPfe.entity.course.LessonType.TEXT) {
                                        lesson.setVideoUrl(null);
                                        lesson.setVideoSize(null);
                                        lesson.setPdfUrl(null);
                                    }
                                    lessonRepository.save(lesson);
                                    log.info("[AUTO-FIX] Lesson ID={} corrected and saved", lesson.getId());
                                }
                                
                                LessonDto lDto = toLessonDto(lesson);
                                log.info("[STUDENT_VIEW_FRESH] Lesson ID={}, type={}, pdfUrl={}, videoUrl={}, articleContent={}", 
                                         lesson.getId(), lesson.getLessonType(), 
                                         lesson.getPdfUrl() != null ? "present" : "null",
                                         lesson.getVideoUrl() != null ? "present" : "null",
                                         lesson.getArticleContent() != null ? "present" : "null");
                                return lDto;
                            })
                            .collect(Collectors.toList())
                    );
                    return sDto;
                })
                .collect(Collectors.toList())
        );
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
        dto.setArticleContent(normalizeArticleHtmlForDisplay(lesson.getArticleContent()));
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

        // ── VERSIONING : créer snapshot AVANT modification si cours publié ──
        Course course = lesson.getSection().getCourse();
        ensureSnapshotBeforeLessonModification(course);

        lesson.setArticleContent(normalizeArticleHtmlForStorage(content));
        lesson.setLessonType(com.elearning.ProjetPfe.entity.course.LessonType.TEXT);
        // Supprimer l'ancien contenu (vidéo et PDF) pour éviter les conflits
        lesson.setVideoUrl(null);
        lesson.setVideoSize(null);
        lesson.setPdfUrl(null);
        lesson = lessonRepository.save(lesson);
        return toLessonDto(lesson);
    }

    /**
     * Normalise le contenu HTML d'article avant stockage.
     * Corrige les cas legacy où le HTML arrive encodé (&lt;h1&gt;...).
     */
    private String normalizeArticleHtmlForStorage(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return "";
        }

        String normalized = rawContent.replace("\r\n", "\n").replace("\r", "\n").trim();

        // Cas 1: contenu HTML encode complet (legacy) -> decoder.
        if (looksLikeEscapedRichHtml(normalized) && !looksLikeStructuredHtml(normalized)) {
            return HtmlUtils.htmlUnescape(normalized);
        }

        // Cas 2: contenu texte brut (copier/coller) -> convertir en HTML securise.
        // Important: on echappe tout pour que les snippets <h1> restent du texte.
        if (!looksLikeStructuredHtml(normalized)) {
            return plainTextToHtml(normalized);
        }

        return normalized;
    }

    /**
     * Normalise le contenu HTML pour affichage DTO.
     * Permet de rendre correctement les articles legacy deja stockes encodes.
     */
    private String normalizeArticleHtmlForDisplay(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return rawContent;
        }

        if (looksLikeEscapedRichHtml(rawContent) && !looksLikeStructuredHtml(rawContent)) {
            return HtmlUtils.htmlUnescape(rawContent);
        }

        return rawContent;
    }

    private boolean looksLikeEscapedRichHtml(String value) {
        String lower = value.toLowerCase();
        return lower.contains("&lt;p")
                || lower.contains("&lt;div")
                || lower.contains("&lt;h1")
                || lower.contains("&lt;h2")
                || lower.contains("&lt;h3")
                || lower.contains("&lt;ul")
                || lower.contains("&lt;ol")
                || lower.contains("&lt;li")
                || lower.contains("&lt;blockquote")
                || lower.contains("&lt;pre")
                || lower.contains("&lt;span");
    }

    private boolean looksLikeStructuredHtml(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty() || !trimmed.startsWith("<")) {
            return false;
        }
        return trimmed.matches("(?s).*<\\s*/?\\s*[a-zA-Z][^>]*>.*");
    }

    private String plainTextToHtml(String value) {
        String escaped = HtmlUtils.htmlEscape(value);
        // Preserver la structure des paragraphes et les retours ligne intra-paragraphe.
        String[] paragraphs = escaped.split("\\n\\s*\\n");
        StringBuilder html = new StringBuilder();
        for (String paragraph : paragraphs) {
            String p = paragraph.trim();
            if (p.isEmpty()) {
                continue;
            }
            if (html.length() > 0) {
                html.append('\n');
            }
            html.append("<p>").append(p.replace("\n", "<br>")).append("</p>");
        }
        return html.toString();
    }
}