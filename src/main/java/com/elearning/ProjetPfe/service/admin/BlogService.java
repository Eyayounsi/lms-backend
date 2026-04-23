package com.elearning.ProjetPfe.service.admin;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elearning.ProjetPfe.dto.admin.BlogPostDto;
import com.elearning.ProjetPfe.entity.admin.BlogPost;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.admin.BlogPostRepository;

@Service
public class BlogService {

    @Autowired
    private BlogPostRepository blogPostRepository;

    public List<BlogPostDto> getAllPosts() {
        return blogPostRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<BlogPostDto> getPublishedPosts() {
        return blogPostRepository.findByStatusOrderByCreatedAtDesc("PUBLISHED")
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public BlogPostDto getPost(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article introuvable"));
        return toDto(post);
    }

    @Transactional
    public BlogPostDto createPost(BlogPostDto dto, User author) {
        if (dto.getTitle() == null || dto.getTitle().isBlank())
            throw new RuntimeException("Le titre est obligatoire");
        if (dto.getContent() == null || dto.getContent().isBlank())
            throw new RuntimeException("Le contenu est obligatoire");

        BlogPost post = new BlogPost();
        post.setTitle(dto.getTitle().trim());
        post.setSummary(dto.getSummary());
        post.setContent(dto.getContent());
        post.setCoverImage(dto.getCoverImage());
        post.setStatus("DRAFT");
        post.setAuthor(author);
        // generate slug from title
        post.setSlug(generateSlug(dto.getTitle()));
        return toDto(blogPostRepository.save(post));
    }

    @Transactional
    public BlogPostDto updatePost(Long id, BlogPostDto dto) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article introuvable"));

        if (dto.getTitle() != null && !dto.getTitle().isBlank())
            post.setTitle(dto.getTitle().trim());
        if (dto.getSummary() != null)
            post.setSummary(dto.getSummary());
        if (dto.getContent() != null && !dto.getContent().isBlank())
            post.setContent(dto.getContent());
        if (dto.getCoverImage() != null)
            post.setCoverImage(dto.getCoverImage());

        return toDto(blogPostRepository.save(post));
    }

    @Transactional
    public BlogPostDto togglePublish(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article introuvable"));
        post.setStatus("PUBLISHED".equals(post.getStatus()) ? "DRAFT" : "PUBLISHED");
        return toDto(blogPostRepository.save(post));
    }

    @Transactional
    public void deletePost(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article introuvable"));
        blogPostRepository.delete(post);
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-") + "-" + System.currentTimeMillis();
    }

    private BlogPostDto toDto(BlogPost p) {
        BlogPostDto dto = new BlogPostDto();
        dto.setId(p.getId());
        dto.setTitle(p.getTitle());
        dto.setSlug(p.getSlug());
        dto.setSummary(p.getSummary());
        dto.setContent(p.getContent());
        dto.setCoverImage(p.getCoverImage());
        dto.setStatus(p.getStatus());
        dto.setAuthorName(p.getAuthor() != null ? p.getAuthor().getFullName() : "Admin");
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }
}
