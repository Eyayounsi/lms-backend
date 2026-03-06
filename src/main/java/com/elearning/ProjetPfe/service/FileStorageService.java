package com.elearning.ProjetPfe.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service de stockage de fichiers sur le serveur.
 *
 * Structure des dossiers :
 *   uploads/
 *     covers/   → images de couverture des cours
 *     videos/   → vidéos des leçons
 *     pdfs/     → fichiers PDF des leçons
 *
 * Chaque fichier est renommé avec un UUID pour éviter les doublons.
 */
@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /** Taille max vidéo : 500 MB */
    private static final long MAX_VIDEO_SIZE = 500L * 1024 * 1024;

    /**
     * Stocker un fichier sur le disque.
     *
     * @param file     le fichier à sauvegarder
     * @param subDir   le sous-dossier (covers, videos, pdfs)
     * @return         le chemin relatif du fichier sauvegardé (ex: /uploads/videos/abc-123.mp4)
     */
    public String storeFile(MultipartFile file, String subDir) {
        try {
            // Créer le dossier si inexistant
            Path dirPath = Paths.get(uploadDir, subDir);
            Files.createDirectories(dirPath);

            // Générer un nom unique (UUID + extension originale)
            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + extension;

            // Copier le fichier
            Path filePath = dirPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Retourner le chemin relatif pour stocker en base
            return "/" + uploadDir + "/" + subDir + "/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException("Impossible de sauvegarder le fichier: " + e.getMessage());
        }
    }

    /**
     * Vérifier que la vidéo ne dépasse pas la taille max.
     */
    public void validateVideoSize(MultipartFile file) {
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new RuntimeException(
                "La vidéo dépasse la limite de 500 MB. Taille: "
                + (file.getSize() / (1024 * 1024)) + " MB"
            );
        }
    }

    /**
     * Supprimer un fichier du disque.
     */
    public void deleteFile(String filePath) {
        try {
            if (filePath != null) {
                // Retirer le "/" en début pour obtenir le chemin relatif
                Path path = Paths.get(filePath.startsWith("/") ? filePath.substring(1) : filePath);
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            System.err.println("Impossible de supprimer le fichier: " + e.getMessage());
        }
    }
}
