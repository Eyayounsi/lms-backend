package com.elearning.ProjetPfe.service.course;

import com.elearning.ProjetPfe.entity.course.CourseStatus;
import com.elearning.ProjetPfe.entity.learning.Note;
import com.elearning.ProjetPfe.entity.course.Course;
import com.elearning.ProjetPfe.dto.course.CategoryDto;
import com.elearning.ProjetPfe.entity.course.Category;
import com.elearning.ProjetPfe.repository.course.CategoryRepository;
import com.elearning.ProjetPfe.repository.course.CourseRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Logique métier pour les catégories de cours.
 *
 * ═══════════════════════════════════════════════════
 * QUI PEUT FAIRE QUOI ?
 * ═══════════════════════════════════════════════════
 *   Public (sans token)  → listAll, getBySlug
 *   ADMIN / SUPERADMIN   → create, update, delete
 *
 * ═══════════════════════════════════════════════════
 * SLUG — POURQUOI ? COMMENT ?
 * ═══════════════════════════════════════════════════
 *   Le slug est une version URL-friendly du nom.
 *   Ex: "Développement Web" → "developpement-web"
 *
 *   Algorithme :
 *     1. toLowerCase()
 *     2. Remplacer les accents (é→e, è→e, ç→c...)
 *     3. Remplacer les espaces et caractères spéciaux par "-"
 *     4. Supprimer les "-" doubles
 *
 *   Pourquoi utile ?
 *   → URL propre : /courses?category=developpement-web
 *   → Meilleur SEO que /courses?category=3
 */
@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CourseRepository courseRepository;

    // ═══════════════════════════════════════════════════════════════════════
    //  PUBLIC — Liste de toutes les catégories
    // ═══════════════════════════════════════════════════════════════════════

    @Cacheable(cacheNames = "publicCategories")
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PUBLIC — Chercher par slug
    // ═══════════════════════════════════════════════════════════════════════

    public CategoryDto getBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée : " + slug));
        return toDto(category);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN — Créer une catégorie
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public CategoryDto createCategory(CategoryDto dto) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Une catégorie avec ce nom existe déjà");
        }

        Category category = new Category();
        category.setName(dto.getName());
        category.setSlug(generateSlug(dto.getName()));
        category.setIcon(dto.getIcon());
        category.setDescription(dto.getDescription());
        category.setDisplayOrder(dto.getDisplayOrder());

        category = categoryRepository.save(category);
        return toDto(category);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN — Modifier une catégorie
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public CategoryDto updateCategory(Long id, CategoryDto dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));

        category.setName(dto.getName());
        category.setSlug(generateSlug(dto.getName()));
        category.setIcon(dto.getIcon());
        category.setDescription(dto.getDescription());
        category.setDisplayOrder(dto.getDisplayOrder());

        category = categoryRepository.save(category);
        return toDto(category);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN — Supprimer une catégorie
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Catégorie non trouvée");
        }
        categoryRepository.deleteById(id);
        // Note : les cours avec cette catégorie auront category = null (nullable)
        // On n'efface pas les cours, seulement le lien vers la catégorie
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CONVERSIONS
    // ═══════════════════════════════════════════════════════════════════════

    private CategoryDto toDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setSlug(category.getSlug());
        dto.setIcon(category.getIcon());
        dto.setDescription(category.getDescription());
        dto.setDisplayOrder(category.getDisplayOrder());

        // Nombre de cours PUBLISHED dans cette catégorie
        long count = courseRepository.countByCategoryIdAndStatus(
                category.getId(),
                com.elearning.ProjetPfe.entity.course.CourseStatus.PUBLISHED
        );
        dto.setCourseCount(count);
        return dto;
    }

    /**
     * Génère un slug URL-friendly à partir d'un nom.
     *
     * Exemples :
     *   "Développement Web"  → "developpement-web"
     *   "Data Science & IA"  → "data-science-ia"
     *   "Python 3.10"        → "python-3-10"
     */
    public static String generateSlug(String name) {
        if (name == null) return "";

        String slug = name.toLowerCase()
                // Remplacer les caractères accentués
                .replace("é", "e").replace("è", "e").replace("ê", "e").replace("ë", "e")
                .replace("à", "a").replace("â", "a").replace("ä", "a")
                .replace("ô", "o").replace("ö", "o")
                .replace("ù", "u").replace("û", "u").replace("ü", "u")
                .replace("î", "i").replace("ï", "i")
                .replace("ç", "c")
                // Remplacer tout ce qui n'est pas alphanumérique par "-"
                .replaceAll("[^a-z0-9]+", "-")
                // Supprimer les "-" en début et fin
                .replaceAll("^-+|-+$", "");

        return slug;
    }
}
