package com.elearning.ProjetPfe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Table "lessons" — une leçon dans une section.
 *
 * ═══════════════════════════════════════════════════
 * TYPES DE LEÇONS (lessonType) :
 * ═══════════════════════════════════════════════════
 *   VIDEO → la leçon principale est une vidéo
 *   TEXT  → article/texte (pas de vidéo)
 *   PDF   → document PDF
 *
 * ═══════════════════════════════════════════════════
 * LEÇON GRATUITE (isFree) :
 * ═══════════════════════════════════════════════════
 *   isFree = true → accessible sans paiement (prévisualisation)
 *   Utile pour attirer les étudiants sur votre cours.
 *   Ex: la première leçon de chaque section est souvent gratuite.
 *
 * ═══════════════════════════════════════════════════
 * DURÉE (duration) :
 * ═══════════════════════════════════════════════════
 *   Stockée en secondes pour être plus simple à calculer.
 *   Ex: 125 secondes = 2 min 05 sec
 *   Côté frontend, on la formate : Math.floor(125/60) + ":" + (125%60)
 */
@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    /** Texte explicatif de la leçon (optionnel, affiché sous la vidéo) */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Position dans la section : 0, 1, 2... Trié croissant. */
    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    /** VIDEO, TEXT ou PDF */
    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", length = 10)
    private LessonType lessonType = LessonType.VIDEO;

    /**
     * Durée de la leçon en secondes.
     * Renseignée automatiquement ou manuellement par l'instructor.
     */
    @Column(name = "duration_seconds")
    private Long durationSeconds;

    /**
     * true = cette leçon est accessible sans paiement (prévisualisation).
     * Permet aux visiteurs de voir un aperçu avant d'acheter.
     */
    @Column(name = "is_free")
    private boolean free = false;

    // ─── CONTENU ──────────────────────────────────────────────────────────

    /** Chemin de la vidéo (ex: uploads/videos/uuid.mp4). null si pas de vidéo. */
    @Column(name = "video_url")
    private String videoUrl;

    /** Taille de la vidéo en octets — pour afficher "450 MB" */
    @Column(name = "video_size")
    private Long videoSize;

    /** Chemin du PDF (ex: uploads/pdfs/uuid.pdf). null si pas de PDF. */
    @Column(name = "pdf_url")
    private String pdfUrl;

    /**
     * Contenu HTML riche d'un article (type TEXT).
     * Stocké tel quel, affiché avec [innerHTML] côté frontend.
     */
    @Column(name = "article_content", columnDefinition = "LONGTEXT")
    private String articleContent;

    // ─── RELATION ─────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    // ─── CONSTRUCTEUR ─────────────────────────────────────────────────────

    public Lesson() {}

    // ─── GETTERS & SETTERS ────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public LessonType getLessonType() { return lessonType; }
    public void setLessonType(LessonType lessonType) { this.lessonType = lessonType; }

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

    public Section getSection() { return section; }
    public void setSection(Section section) { this.section = section; }
}
