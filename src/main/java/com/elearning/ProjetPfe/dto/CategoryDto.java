package com.elearning.ProjetPfe.dto;

/**
 * DTO pour afficher et créer une catégorie.
 *
 * Utilisé par :
 *   - GET  /api/public/categories → liste toutes les catégories
 *   - POST /api/admin/categories  → créer une catégorie (admin seulement)
 *   - PUT  /api/admin/categories/{id} → modifier
 */
public class CategoryDto {

    private Long id;

    /** Nom affiché (ex: "Développement Web") */
    private String name;

    /** Slug URL (ex: "developpement-web") — généré automatiquement depuis name */
    private String slug;

    /** Classe CSS ou URL d'icône (ex: "bx bx-code-alt" ou "/images/icons/web.png") */
    private String icon;

    /** Description courte de la catégorie */
    private String description;

    /** Ordre d'affichage sur la page d'accueil */
    private int displayOrder;

    /** Nombre de cours publiés dans cette catégorie (calculé à la volée) */
    private Long courseCount;

    public CategoryDto() {}

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public Long getCourseCount() { return courseCount; }
    public void setCourseCount(Long courseCount) { this.courseCount = courseCount; }
}
