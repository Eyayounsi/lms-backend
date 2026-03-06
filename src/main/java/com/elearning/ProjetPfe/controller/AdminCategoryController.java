package com.elearning.ProjetPfe.controller;

import com.elearning.ProjetPfe.dto.CategoryDto;
import com.elearning.ProjetPfe.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints pour les catégories.
 *
 * ─── PUBLIC (sans token) ──────────────────────────────────────────────────
 *   GET  /api/public/categories           → liste toutes les catégories
 *   GET  /api/public/categories/{slug}    → catégorie par slug
 *
 * ─── ADMIN seulement ─────────────────────────────────────────────────────
 *   POST   /api/admin/categories          → créer une catégorie
 *   PUT    /api/admin/categories/{id}     → modifier
 *   DELETE /api/admin/categories/{id}     → supprimer
 *
 * Note : les routes /api/public/** sont accessibles sans authentification
 * (configuré dans SecurityConfig)
 */
@RestController
public class AdminCategoryController {

    @Autowired
    private CategoryService categoryService;

    // ─── PUBLIC ───────────────────────────────────────────────────────────

    @GetMapping("/api/public/categories")
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/api/public/categories/{slug}")
    public ResponseEntity<CategoryDto> getCategoryBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(categoryService.getBySlug(slug));
    }

    // ─── ADMIN ────────────────────────────────────────────────────────────

    @PostMapping("/api/admin/categories")
    public ResponseEntity<CategoryDto> createCategory(@RequestBody CategoryDto dto) {
        return ResponseEntity.ok(categoryService.createCategory(dto));
    }

    @PutMapping("/api/admin/categories/{id}")
    public ResponseEntity<CategoryDto> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryDto dto) {
        return ResponseEntity.ok(categoryService.updateCategory(id, dto));
    }

    @DeleteMapping("/api/admin/categories/{id}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok("Catégorie supprimée");
    }
}
