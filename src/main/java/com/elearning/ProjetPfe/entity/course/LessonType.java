package com.elearning.ProjetPfe.entity.course;

/**
 * Type d'une leçon.
 *
 * VIDEO → la leçon principale est une vidéo uploadée (mp4, webm...)
 *         Le frontend affiche un lecteur vidéo HTML5.
 *
 * TEXT  → la leçon est un article/texte (contenu stocké en base ou Markdown)
 *         Utile pour les leçons théoriques sans vidéo.
 *
 * PDF   → la leçon est un document PDF à lire/télécharger
 *         Le frontend affiche un visionneur PDF.
 *
 * Pourquoi un type ? Parce que le frontend doit savoir QUOI afficher.
 * Chaque type a une UI différente (lecteur vidéo vs. texte vs. PDF viewer).
 */
public enum LessonType {
    VIDEO,
    TEXT,
    PDF
}
