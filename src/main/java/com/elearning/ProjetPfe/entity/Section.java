package com.elearning.ProjetPfe.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Table "sections" — un chapitre d'un cours.
 *
 * Relations :
 *   - ManyToOne → Course (la section appartient à 1 cours)
 *   - OneToMany → Lesson (la section contient N leçons)
 *
 * orderIndex = position de la section dans le cours (0, 1, 2...)
 */
@Entity
@Table(name = "sections")
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    /** Position de la section dans le cours (pour afficher dans l'ordre) */
    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    // ─── RELATION : Cette section appartient à quel cours ? ───────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // ─── RELATION : Les leçons de cette section ──────────────────────────
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Lesson> lessons = new ArrayList<>();

    // ─── Constructeurs ────────────────────────────────────────────────────
    public Section() {}

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public List<Lesson> getLessons() { return lessons; }
    public void setLessons(List<Lesson> lessons) { this.lessons = lessons; }
}
