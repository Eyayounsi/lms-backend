package com.elearning.ProjetPfe.service.content;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service d'extraction de texte depuis les fichiers PDF.
 * Utilisé pour enrichir le contexte du chatbot student avec le contenu réel des leçons PDF.
 */
@Service
public class PdfContentExtractorService {

    private static final Logger log = LoggerFactory.getLogger(PdfContentExtractorService.class);

    @Value("${app.upload.base-path:uploads}")
    private String uploadBasePath;

    /**
     * Extrait le texte complet d'un fichier PDF.
     * 
     * @param pdfPath Chemin relatif du PDF (ex: "pdfs/uuid.pdf")
     * @return Le texte extrait, ou null si erreur
     */
    public String extractTextFromPdf(String pdfPath) {
        if (pdfPath == null || pdfPath.isBlank()) {
            return null;
        }

        try {
            // Construire le chemin absolu
            File pdfFile = new File(uploadBasePath, pdfPath);
            
            if (!pdfFile.exists() || !pdfFile.isFile()) {
                log.warn("[PDF_EXTRACT] Fichier introuvable: {}", pdfFile.getAbsolutePath());
                return null;
            }

            // Extraire le texte avec PDFBox 3.x (nouvelle API avec Loader)
            try (PDDocument document = Loader.loadPDF(pdfFile)) {
                PDFTextStripper stripper = new PDFTextStripper();
                
                // Limiter à 50 pages pour éviter les timeouts sur les gros PDF
                int totalPages = document.getNumberOfPages();
                if (totalPages > 50) {
                    stripper.setEndPage(50);
                    log.info("[PDF_EXTRACT] PDF volumineux ({} pages), extraction limitée aux 50 premières", totalPages);
                }
                
                String text = stripper.getText(document);
                
                // Nettoyer le texte
                text = cleanExtractedText(text);
                
                log.info("[PDF_EXTRACT] Extraction réussie: {} caractères depuis {}", 
                         text.length(), pdfPath);
                
                return text;
                
            } catch (IOException e) {
                log.error("[PDF_EXTRACT] Erreur lors de la lecture du PDF: {}", pdfPath, e);
                return null;
            }
            
        } catch (Exception e) {
            log.error("[PDF_EXTRACT] Erreur inattendue lors de l'extraction: {}", pdfPath, e);
            return null;
        }
    }

    /**
     * Extrait un extrait limité du PDF (pour le chatbot).
     * 
     * @param pdfPath Chemin relatif du PDF
     * @param maxChars Nombre maximum de caractères à extraire
     * @return L'extrait du texte, ou null si erreur
     */
    public String extractTextExcerpt(String pdfPath, int maxChars) {
        String fullText = extractTextFromPdf(pdfPath);
        
        if (fullText == null || fullText.isEmpty()) {
            return null;
        }
        
        if (fullText.length() <= maxChars) {
            return fullText;
        }
        
        // Tronquer intelligemment (couper à la fin d'une phrase si possible)
        String excerpt = fullText.substring(0, maxChars);
        int lastPeriod = excerpt.lastIndexOf('.');
        int lastNewline = excerpt.lastIndexOf('\n');
        
        int cutPoint = Math.max(lastPeriod, lastNewline);
        if (cutPoint > maxChars * 0.8) { // Si on trouve un point dans les derniers 20%
            excerpt = excerpt.substring(0, cutPoint + 1);
        }
        
        return excerpt.trim();
    }

    /**
     * Nettoie le texte extrait du PDF (supprime les espaces multiples, lignes vides, etc.)
     */
    private String cleanExtractedText(String text) {
        if (text == null) {
            return "";
        }
        
        // Supprimer les espaces multiples
        text = text.replaceAll("[ \\t]+", " ");
        
        // Supprimer les lignes vides multiples
        text = text.replaceAll("\\n{3,}", "\n\n");
        
        // Supprimer les espaces en début/fin de lignes
        text = text.replaceAll("(?m)^[ \\t]+|[ \\t]+$", "");
        
        return text.trim();
    }

    /**
     * Vérifie si un PDF est lisible et contient du texte.
     * 
     * @param pdfPath Chemin relatif du PDF
     * @return true si le PDF est lisible et contient du texte
     */
    public boolean isPdfReadable(String pdfPath) {
        String text = extractTextExcerpt(pdfPath, 100);
        return text != null && !text.isBlank();
    }
}
