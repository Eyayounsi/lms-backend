package com.elearning.ProjetPfe.controller.admin;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.elearning.ProjetPfe.dto.admin.BlogPostDto;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.auth.UserRepository;
import com.elearning.ProjetPfe.service.admin.BlogService;
import com.elearning.ProjetPfe.service.course.FileStorageService;

/**
 * GET    /api/admin/blog          → liste tous les articles
 * GET    /api/admin/blog/{id}     → détail d'un article
 * POST   /api/admin/blog          → créer un article
 * PUT    /api/admin/blog/{id}     → modifier un article
 * PUT    /api/admin/blog/{id}/toggle → publier / dépublier
 * DELETE /api/admin/blog/{id}     → supprimer
 */
@RestController
@RequestMapping("/api/admin/blog")
public class AdminBlogController {

    @Autowired private BlogService blogService;
    @Autowired private UserRepository userRepository;
    @Autowired private FileStorageService fileStorageService;

    private User getAdmin(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    @GetMapping
    public ResponseEntity<List<BlogPostDto>> getAll() {
        return ResponseEntity.ok(blogService.getAllPosts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlogPostDto> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(blogService.getPost(id));
    }

    @PostMapping
    public ResponseEntity<BlogPostDto> create(@RequestBody BlogPostDto dto, Authentication auth) {
        return ResponseEntity.ok(blogService.createPost(dto, getAdmin(auth)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BlogPostDto> update(@PathVariable Long id, @RequestBody BlogPostDto dto) {
        return ResponseEntity.ok(blogService.updatePost(id, dto));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<BlogPostDto> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(blogService.togglePublish(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        blogService.deletePost(id);
        return ResponseEntity.ok("Article supprimé");
    }

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Fichier vide"));
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Seules les images sont acceptées"));
        }
        String path = fileStorageService.storeFile(file, "blog-covers");
        String url = "http://localhost:8081" + path;
        return ResponseEntity.ok(Map.of("url", url));
    }
}
