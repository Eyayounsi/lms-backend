package com.elearning.ProjetPfe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Table "categories" — les catégories de cours.
 *
 * Exemples : "Développement Web", "Data Science", "Design", "Business"
 *
 * Relation :
 *   OneToMany → Course (une catégorie contient plusieurs cours)
 *   C'est la relation inverse : Course a un ManyToOne vers Category.
 *
 * ═══════════════════════════════════════════════════
 * POURQUOI UN SLUG ?
 * ═══════════════════════════════════════════════════
 *   slug = version URL-friendly du nom.
 *   Exemple :
 *     name = "Développement Web"
 *     slug = "developpement-web"
 *
 *   Avantages :
 *   1. URL lisible : /courses/category/developpement-web
 *   2. Meilleur SEO (référencement Google)
 *   3. Stable même si on change le nom affiché
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom affiché à l'utilisateur, doit être unique */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * Version URL-friendly du nom.
     * Exemple : "Développement Web" → "developpement-web"
     * Unique car on l'utilise dans les URLs.
     */
    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    /**
     * Identifiant d'icône (optionnel).
     * Peut être un nom de classe CSS (ex: "fa-code") ou une URL d'image.
     * On utilise des icônes Font Awesome dans le frontend.
     */
    @Column(name = "icon", length = 100)
    private String icon;

    /**
     * Description courte de la catégorie.
     * Affichée sur la page de la catégorie.
     */
    @Column(length = 500)
    private String description;

    /**
     * Ordre d'affichage dans les menus/filtres.
     * 0 = affiché en premier.
     */
    @Column(name = "display_order")
    private int displayOrder = 0;

    // ─── Constructeurs ────────────────────────────────────────────────────

    public Category() {}

    public Category(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

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
}
