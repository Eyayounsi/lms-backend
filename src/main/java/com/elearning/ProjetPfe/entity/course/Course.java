package com.elearning.ProjetPfe.entity.course;

import com.elearning.ProjetPfe.entity.auth.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Table "courses" — un cours créé par un instructor.
 *
 * CYCLE DE VIE des statuts :
 *   DRAFT  → PENDING (instructor soumet)
 *   PENDING → PUBLISHED (admin approuve) | REJECTED (admin rejette)
 *   REJECTED → DRAFT (instructor corrige)
 *   PUBLISHED → ARCHIVED (retrait de la vente, accès conservé pour anciens étudiants)
 */
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── INFORMATIONS DE BASE ─────────────────────────────────────────────

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** "À la fin de ce cours, vous saurez..." — affiché sur la page du cours */
    @Column(columnDefinition = "TEXT")
    private String objectives;

    /** "Avant de suivre ce cours, vous devez savoir..." */
    @Column(columnDefinition = "TEXT")
    private String requirements;

    /** Langue du cours : "Français", "English", "العربية"... */
    @Column(length = 50)
    private String language = "Français";

    /** Durée totale estimée en secondes — recalculée quand on ajoute des leçons */
    @Column(name = "total_duration_seconds")
    private Long totalDurationSeconds = 0L;

    // ─── IMAGE DE COUVERTURE ──────────────────────────────────────────────

    @Column(name = "cover_image")
    private String coverImage;

    // ─── NIVEAU ───────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseLevel level = CourseLevel.BEGINNER;

    // ─── PRIX ET PROMOTION ────────────────────────────────────────────────

    /**
     * Prix normal. BigDecimal évite les erreurs d'arrondi des float/double.
     * Ex: 0.1 + 0.2 = 0.30000000004 avec double → BigDecimal règle ça.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Prix réduit (null = pas de promo).
     * La méthode getEffectivePrice() décide lequel afficher.
     */
    @Column(name = "discount_price", precision = 10, scale = 2)
    private BigDecimal discountPrice;

    /** Fin de la promotion (null = promo sans date limite) */
    @Column(name = "discount_ends_at")
    private LocalDateTime discountEndsAt;

    // ─── STATUT ET VALIDATION ─────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private CourseStatus status = CourseStatus.DRAFT;

    /** Raison du rejet écrite par l'admin — null si jamais rejeté */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /** true = affiché en avant sur la home page (décidé par l'admin) */
    @Column(name = "is_featured", columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean featured = false;

    // ─── VERSIONING — Modification en attente pour cours publiés ──────────

    /**
     * JSON contenant les modifications proposées par l'instructor sur un cours PUBLISHED.
     * Les étudiants continuent de voir les données actuelles tant que l'admin n'a pas approuvé.
     * null = pas de modification en attente.
     */
    @Column(name = "pending_edit", columnDefinition = "TEXT")
    private String pendingEdit;

    /** true = l'instructor a soumis une modification en attente de validation admin */
    @Column(name = "has_pending_edit", columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean hasPendingEdit = false;

    /** Raison du rejet de la modification (optionnel) */
    @Column(name = "edit_rejection_reason", columnDefinition = "TEXT")
    private String editRejectionReason;

    // ─── RELATIONS ────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    /**
     * FetchType.LAZY : on ne charge pas la catégorie tant qu'on n'y accède pas.
     * nullable : les cours existants en DB n'ont pas encore de catégorie.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Section> sections = new ArrayList<>();

    // ─── TIMESTAMPS ───────────────────────────────────────────────────────

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Date à laquelle l'admin a approuvé le cours */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ─── MÉTHODES MÉTIER ──────────────────────────────────────────────────

    /**
     * Prix effectif à afficher selon la présence d'une promo active.
     * La logique métier est dans l'entité, pas dans le service ni le controller.
     */
    public BigDecimal getEffectivePrice() {
        if (discountPrice != null && discountPrice.compareTo(price) < 0) {
            if (discountEndsAt == null || LocalDateTime.now().isBefore(discountEndsAt)) {
                return discountPrice;
            }
        }
        return price;
    }

    public boolean isOnSale() {
        return !getEffectivePrice().equals(price);
    }

    // ─── CONSTRUCTEUR ─────────────────────────────────────────────────────

    public Course() {}

    // ─── GETTERS & SETTERS ────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getObjectives() { return objectives; }
    public void setObjectives(String objectives) { this.objectives = objectives; }

    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Long getTotalDurationSeconds() { return totalDurationSeconds; }
    public void setTotalDurationSeconds(Long v) { this.totalDurationSeconds = v; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    public CourseLevel getLevel() { return level; }
    public void setLevel(CourseLevel level) { this.level = level; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getDiscountPrice() { return discountPrice; }
    public void setDiscountPrice(BigDecimal discountPrice) { this.discountPrice = discountPrice; }

    public LocalDateTime getDiscountEndsAt() { return discountEndsAt; }
    public void setDiscountEndsAt(LocalDateTime discountEndsAt) { this.discountEndsAt = discountEndsAt; }

    public CourseStatus getStatus() { return status; }
    public void setStatus(CourseStatus status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String r) { this.rejectionReason = r; }

    public Boolean isFeatured() { return featured != null ? featured : false; }
    public void setFeatured(Boolean featured) { this.featured = featured != null ? featured : false; }

    public User getInstructor() { return instructor; }
    public void setInstructor(User instructor) { this.instructor = instructor; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public List<Section> getSections() { return sections; }
    public void setSections(List<Section> sections) { this.sections = sections; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public String getPendingEdit() { return pendingEdit; }
    public void setPendingEdit(String pendingEdit) { this.pendingEdit = pendingEdit; }

    public boolean isHasPendingEdit() { return hasPendingEdit; }
    public void setHasPendingEdit(boolean hasPendingEdit) { this.hasPendingEdit = hasPendingEdit; }

    public String getEditRejectionReason() { return editRejectionReason; }
    public void setEditRejectionReason(String editRejectionReason) { this.editRejectionReason = editRejectionReason; }
}
