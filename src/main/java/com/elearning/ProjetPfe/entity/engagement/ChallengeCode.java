package com.elearning.ProjetPfe.entity.engagement;

import com.elearning.ProjetPfe.entity.learning.Quiz;
/**
 * Codes des 10 challenges par défaut créés pour chaque étudiant.
 * V2 : challenges équilibrés axés sur la progression d'apprentissage.
 */
public enum ChallengeCode {
    FIRST_PURCHASE,       // Acheter son premier cours
    FIRST_LESSON,         // Compléter sa première leçon
    COMPLETE_10_LESSONS,  // Compléter 10 leçons
    COMPLETE_25_LESSONS,  // Compléter 25 leçons
    FINISH_FIRST_COURSE,  // Terminer un cours à 100%
    FINISH_3_COURSES,     // Terminer 3 cours à 100%
    BUY_5_COURSES,        // Acheter 5 cours
    PASS_5_QUIZZES,       // Réussir 5 quiz
    ASK_3_QUESTIONS,      // Poser 3 questions dans le Q&A
    COMPLETE_PROFILE      // Compléter son profil
}
