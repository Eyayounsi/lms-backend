package com.elearning.ProjetPfe.dto;

import java.util.List;

/**
 * DTO pour afficher une section et ses leçons.
 */
public class SectionDto {

    private Long id;
    private String title;
    private int orderIndex;
    private List<LessonDto> lessons;

    public SectionDto() {}

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public List<LessonDto> getLessons() { return lessons; }
    public void setLessons(List<LessonDto> lessons) { this.lessons = lessons; }
}
