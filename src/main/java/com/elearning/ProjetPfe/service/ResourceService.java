package com.elearning.ProjetPfe.service;

import com.elearning.ProjetPfe.dto.ResourceDto;
import com.elearning.ProjetPfe.entity.Lesson;
import com.elearning.ProjetPfe.entity.Resource;
import com.elearning.ProjetPfe.entity.ResourceType;
import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.repository.LessonRepository;
import com.elearning.ProjetPfe.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResourceService {

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private LessonRepository lessonRepository;

    /** Récupère toutes les ressources d'une leçon */
    public List<ResourceDto> getByLesson(Long lessonId) {
        return resourceRepository.findByLessonId(lessonId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /** Ajoute une ressource à une leçon */
    public ResourceDto addResource(Long lessonId, ResourceDto dto, User instructor) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leçon introuvable"));

        // Vérification propriété
        if (!lesson.getSection().getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }

        Resource resource = new Resource();
        resource.setLesson(lesson);
        resource.setTitle(dto.getTitle());
        resource.setType(ResourceType.valueOf(dto.getType().toUpperCase()));
        resource.setUrl(dto.getUrl());

        return toDto(resourceRepository.save(resource));
    }

    /** Supprime une ressource */
    public void deleteResource(Long resourceId, User instructor) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ressource introuvable"));

        if (!resource.getLesson().getSection().getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }

        resourceRepository.delete(resource);
    }

    private ResourceDto toDto(Resource r) {
        ResourceDto dto = new ResourceDto();
        dto.setId(r.getId());
        dto.setLessonId(r.getLesson().getId());
        dto.setTitle(r.getTitle());
        dto.setType(r.getType().name());
        dto.setUrl(r.getUrl());
        if (r.getCreatedAt() != null) {
            dto.setCreatedAt(r.getCreatedAt().toString());
        }
        return dto;
    }
}
