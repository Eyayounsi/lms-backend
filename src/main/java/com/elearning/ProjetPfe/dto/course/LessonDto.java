package com.elearning.ProjetPfe.dto.course;

import com.elearning.ProjetPfe.entity.course.LessonType;
import com.elearning.ProjetPfe.entity.learning.Quiz;
import com.elearning.ProjetPfe.entity.course.Course;
/**
 * DTO pour afficher une leçon dans le curriculum.
 *
 * ═══════════════════════════════════════════════════
 * SUR LA PAGE PUBLIQUE DU COURS :
 * ═══════════════════════════════════════════════════
 *   - title, lessonType, durationSeconds, free → TOUJOURS affichés
 *   - videoUrl, pdfUrl → masqués (null) si le cours n'est pas acheté
 *     et que free = false
 *
 * ═══════════════════════════════════════════════════
 * AFFICHAGE DURÉE CÔTÉ FRONTEND :
 * ═══════════════════════════════════════════════════
 *   Si durationSeconds = 245 :
 *     const min = Math.floor(245 / 60);  // 4
 *     const sec = 245 % 60;              // 5
 *     display = `${min}:${sec.toString().padStart(2, '0')}`; // "4:05"
 */
public class LessonDto {

    private Long id;
    private String title;
    private String description;
    private int orderIndex;

    /** "VIDEO", "TEXT", ou "PDF" */
    private String lessonType;

    /** Durée en secondes (null si pas encore renseignée) */
    private Long durationSeconds;

    /** true = accessible sans paiement (prévisualisation) */
    private boolean free;

    // ─── Contenu (peut être null pour les non-inscrits) ───────────────────
    private String videoUrl;
    private Long videoSize;
    private String pdfUrl;

    /** Contenu HTML riche pour les leçons de type TEXT/ARTICLE */
    private String articleContent;

    /** true si cette leçon a un quiz associé */
    private boolean hasQuiz;

    /** ID du quiz associé (null si aucun quiz) */
    private Long quizId;

    /** Titre du quiz associé */
    private String quizTitle;

    public LessonDto() {}

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public String getLessonType() { return lessonType; }
    public void setLessonType(String lessonType) { this.lessonType = lessonType; }

    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }

    public boolean isFree() { return free; }
    public void setFree(boolean free) { this.free = free; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public Long getVideoSize() { return videoSize; }
    public void setVideoSize(Long videoSize) { this.videoSize = videoSize; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    public String getArticleContent() { return articleContent; }
    public void setArticleContent(String articleContent) { this.articleContent = articleContent; }

    public boolean isHasQuiz() { return hasQuiz; }
    public void setHasQuiz(boolean hasQuiz) { this.hasQuiz = hasQuiz; }

    public Long getQuizId() { return quizId; }
    public void setQuizId(Long quizId) { this.quizId = quizId; }

    public String getQuizTitle() { return quizTitle; }
    public void setQuizTitle(String quizTitle) { this.quizTitle = quizTitle; }
}
