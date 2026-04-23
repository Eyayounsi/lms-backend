package com.elearning.ProjetPfe.controller.communication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.dto.chatbot.AiAgentRequest;
import com.elearning.ProjetPfe.dto.chatbot.AiAgentResponse;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.entity.course.Course;
import com.elearning.ProjetPfe.entity.course.Lesson;
import com.elearning.ProjetPfe.entity.course.LessonType;
import com.elearning.ProjetPfe.entity.course.Section;
import com.elearning.ProjetPfe.repository.course.CourseRepository;
import com.elearning.ProjetPfe.repository.course.LessonRepository;
import com.elearning.ProjetPfe.repository.course.SectionRepository;
import com.elearning.ProjetPfe.service.communication.PublicChatbotService;
import com.elearning.ProjetPfe.service.content.PdfContentExtractorService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/ai/chatbot")
public class AiChatbotController {

    private final PublicChatbotService publicChatbotService;
    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final PdfContentExtractorService pdfContentExtractorService;

    public AiChatbotController(PublicChatbotService publicChatbotService,
                                CourseRepository courseRepository,
                                SectionRepository sectionRepository,
                                LessonRepository lessonRepository,
                                PdfContentExtractorService pdfContentExtractorService) {
        this.publicChatbotService = publicChatbotService;
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
        this.lessonRepository = lessonRepository;
        this.pdfContentExtractorService = pdfContentExtractorService;
    }

    /**
     * Chatbot tuteur étudiant — enrichit automatiquement le contexte avec
     * les données complètes du cours (structure, objectifs, prérequis, niveau)
     * depuis la base de données avant d'appeler le service Python.
     */
    @PostMapping("/student/tutor")
    public ResponseEntity<AiAgentResponse> studentTutor(
            @Valid @RequestBody AiAgentRequest request,
            @AuthenticationPrincipal User student) {

        // Enrichir le contexte avec les données du cours depuis la DB
        enrichStudentTutorContext(request);

        return ResponseEntity.ok(publicChatbotService.studentTutor(request));
    }

    @PostMapping("/instructor/quiz")
    public ResponseEntity<AiAgentResponse> instructorQuiz(@Valid @RequestBody AiAgentRequest request) {
        return ResponseEntity.ok(publicChatbotService.instructorQuiz(request));
    }

    @PostMapping("/instructor/plan")
    public ResponseEntity<AiAgentResponse> instructorPlan(@Valid @RequestBody AiAgentRequest request) {
        return ResponseEntity.ok(publicChatbotService.instructorPlan(request));
    }

    @PostMapping("/instructor/copilot")
    public ResponseEntity<AiAgentResponse> instructorCopilot(@Valid @RequestBody AiAgentRequest request) {
        return ResponseEntity.ok(publicChatbotService.instructorCopilot(request));
    }

    @PostMapping("/admin/copilot")
    public ResponseEntity<AiAgentResponse> adminCopilot(@Valid @RequestBody AiAgentRequest request) {
        return ResponseEntity.ok(publicChatbotService.adminCopilot(request));
    }

    /**
     * Endpoint admin : questions fréquentes posées par les étudiants au chatbot.
     * Délègue au service Python qui agrège les logs MongoDB.
     */
    @GetMapping("/admin/student-faq-stats")
    public ResponseEntity<Map<String, Object>> getStudentFaqStats(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String courseId) {
        return ResponseEntity.ok(publicChatbotService.getStudentFaqStats(limit, courseId));
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PRIVATE — Enrichissement du contexte étudiant
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Charge les données complètes du cours depuis la DB et les injecte dans
     * le contexte de la requête chatbot pour que le LLM ait un contexte riche.
     *
     * Données ajoutées :
     *   - courseTitle, courseLevel, category, language
     *   - objectives, requirements
     *   - courseStructure (sections + leçons avec types)
     *   - lessonContent (contenu extrait des PDF, articles HTML, ou description vidéo)
     */
    private void enrichStudentTutorContext(AiAgentRequest request) {
        String courseIdStr = request.getCourseId();
        if (courseIdStr == null || courseIdStr.isBlank()) {
            return;
        }

        Long courseId;
        try {
            courseId = Long.parseLong(courseIdStr.trim());
        } catch (NumberFormatException e) {
            return;
        }

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return;
        }

        // Initialiser le contexte si absent
        Map<String, Object> ctx = request.getContext();
        if (ctx == null) {
            ctx = new HashMap<>();
            request.setContext(ctx);
        }

        // LOG: Vérifier si lessonId est présent
        String lessonIdStr = (String) ctx.get("lessonId");
        System.out.println("[CHATBOT_DEBUG] lessonId reçu du frontend: " + lessonIdStr);

        // Métadonnées du cours
        if (!ctx.containsKey("courseTitle") || ctx.get("courseTitle") == null) {
            ctx.put("courseTitle", course.getTitle());
        }
        if (!ctx.containsKey("courseName") || ctx.get("courseName") == null) {
            ctx.put("courseName", course.getTitle());
        }
        ctx.put("courseLevel", course.getLevel() != null ? course.getLevel().name() : "");
        ctx.put("language", course.getLanguage() != null ? course.getLanguage() : "");
        ctx.put("objectives", course.getObjectives() != null ? course.getObjectives() : "");
        ctx.put("requirements", course.getRequirements() != null ? course.getRequirements() : "");

        if (course.getCategory() != null) {
            ctx.put("category", course.getCategory().getName());
            ctx.put("categoryName", course.getCategory().getName());
        }

        // ═══════════════════════════════════════════════════════════════════
        // EXTRACTION DU CONTENU DE LA LEÇON ACTIVE (PDF, ARTICLE, VIDÉO)
        // ═══════════════════════════════════════════════════════════════════
        if (lessonIdStr != null && !lessonIdStr.isBlank()) {
            try {
                Long lessonId = Long.parseLong(lessonIdStr.trim());
                Lesson activeLesson = lessonRepository.findById(lessonId).orElse(null);
                
                if (activeLesson != null) {
                    System.out.println("[CHATBOT_DEBUG] Leçon trouvée: " + activeLesson.getTitle() + " (Type: " + activeLesson.getLessonType() + ")");
                    
                    ctx.put("lessonTitle", activeLesson.getTitle());
                    ctx.put("lessonType", activeLesson.getLessonType() != null ? activeLesson.getLessonType().name() : "");
                    ctx.put("lessonDescription", activeLesson.getDescription() != null ? activeLesson.getDescription() : "");
                    
                    // IMPORTANT: Ajouter le pdfUrl pour permettre l'extraction côté Python si nécessaire
                    if (activeLesson.getLessonType() == LessonType.PDF && activeLesson.getPdfUrl() != null) {
                        ctx.put("pdfUrl", activeLesson.getPdfUrl());
                        System.out.println("[CHATBOT_DEBUG] pdfUrl ajouté au contexte: " + activeLesson.getPdfUrl());
                    }
                    
                    // Extraire le contenu selon le type de leçon
                    String lessonContent = extractLessonContent(activeLesson);
                    if (lessonContent != null && !lessonContent.isBlank()) {
                        System.out.println("[CHATBOT_DEBUG] Contenu extrait: " + lessonContent.length() + " caractères");
                        ctx.put("lessonContent", lessonContent);
                    } else {
                        System.out.println("[CHATBOT_DEBUG] Aucun contenu extrait");
                    }
                } else {
                    System.out.println("[CHATBOT_DEBUG] Leçon non trouvée avec ID: " + lessonId);
                }
            } catch (NumberFormatException e) {
                System.out.println("[CHATBOT_DEBUG] lessonId invalide: " + lessonIdStr);
            }
        } else {
            System.out.println("[CHATBOT_DEBUG] Aucun lessonId fourni - utilisation du fallback");
        }

        // Structure complète du cours (sections + leçons)
        List<Section> sections = sectionRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        List<Map<String, Object>> courseStructure = new ArrayList<>();

        for (Section section : sections) {
            Map<String, Object> sectionMap = new HashMap<>();
            sectionMap.put("id", section.getId());
            sectionMap.put("title", section.getTitle());
            sectionMap.put("orderIndex", section.getOrderIndex());

            List<Lesson> lessons = lessonRepository.findBySectionIdOrderByOrderIndexAsc(section.getId());
            List<Map<String, Object>> lessonList = new ArrayList<>();

            for (Lesson lesson : lessons) {
                Map<String, Object> lessonMap = new HashMap<>();
                lessonMap.put("id", lesson.getId());
                lessonMap.put("title", lesson.getTitle());
                lessonMap.put("lessonType", lesson.getLessonType() != null ? lesson.getLessonType().name() : "VIDEO");
                lessonMap.put("free", lesson.isFree());
                lessonList.add(lessonMap);
            }

            sectionMap.put("lessons", lessonList);
            courseStructure.add(sectionMap);
        }

        ctx.put("courseStructure", courseStructure);
    }

    /**
     * Extrait le contenu textuel d'une leçon selon son type.
     * 
     * @param lesson La leçon à analyser
     * @return Le contenu textuel extrait (max 5000 caractères)
     */
    private String extractLessonContent(Lesson lesson) {
        if (lesson == null) {
            return null;
        }

        switch (lesson.getLessonType()) {
            case TEXT:
                // Article HTML : déjà disponible
                String articleContent = lesson.getArticleContent();
                if (articleContent != null && !articleContent.isBlank()) {
                    // Limiter à 5000 caractères pour ne pas surcharger le LLM
                    return articleContent.length() > 5000 
                        ? articleContent.substring(0, 5000) + "..." 
                        : articleContent;
                }
                break;

            case PDF:
                // Extraire le texte du PDF avec PDFBox
                String pdfUrl = lesson.getPdfUrl();
                if (pdfUrl != null && !pdfUrl.isBlank()) {
                    // Nettoyer le chemin: enlever /uploads/ au début si présent
                    String cleanPdfUrl = pdfUrl;
                    if (cleanPdfUrl.startsWith("/uploads/")) {
                        cleanPdfUrl = cleanPdfUrl.substring(9); // Enlever "/uploads/"
                    } else if (cleanPdfUrl.startsWith("uploads/")) {
                        cleanPdfUrl = cleanPdfUrl.substring(8); // Enlever "uploads/"
                    }
                    
                    System.out.println("[PDF_EXTRACT] Chemin original: " + pdfUrl);
                    System.out.println("[PDF_EXTRACT] Chemin nettoyé: " + cleanPdfUrl);
                    
                    String pdfText = pdfContentExtractorService.extractTextExcerpt(cleanPdfUrl, 5000);
                    if (pdfText != null && !pdfText.isBlank()) {
                        return pdfText;
                    }
                }
                break;

            case VIDEO:
                // Pour les vidéos, utiliser la description + titre
                // TODO: Ajouter la transcription vidéo si disponible
                StringBuilder videoInfo = new StringBuilder();
                videoInfo.append("Vidéo: ").append(lesson.getTitle());
                if (lesson.getDescription() != null && !lesson.getDescription().isBlank()) {
                    videoInfo.append("\n\nDescription: ").append(lesson.getDescription());
                }
                if (lesson.getDurationSeconds() != null) {
                    long minutes = lesson.getDurationSeconds() / 60;
                    long seconds = lesson.getDurationSeconds() % 60;
                    videoInfo.append("\n\nDurée: ").append(minutes).append(" min ").append(seconds).append(" sec");
                }
                return videoInfo.toString();

            default:
                break;
        }

        // Fallback: retourner au moins la description si disponible
        return lesson.getDescription() != null ? lesson.getDescription() : null;
    }
}
