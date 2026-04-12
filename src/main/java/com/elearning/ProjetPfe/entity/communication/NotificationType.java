package com.elearning.ProjetPfe.entity.communication;
/**
 * Types de notifications du système.
 */
public enum NotificationType {
    PURCHASE_CONFIRMED,   // Étudiant : achat confirmé
    COURSE_APPROVED,      // Instructor : cours approuvé par l'admin
    COURSE_REJECTED,      // Instructor : cours rejeté par l'admin
    CERTIFICATE_ISSUED,   // Étudiant : certificat généré
    NEW_REVENUE,          // Instructor : nouveau revenu enregistré
    PAYOUT_PAID,          // Instructor : virement effectué par l'admin
    PAYOUT_REJECTED,      // Instructor : virement rejeté par l'admin
    COURSE_PROMOTION_UPDATED, // Instructor : promotion ajoutée/modifiée/retirée sur son cours
    COURSE_ARCHIVED_BY_ADMIN, // Instructor : cours archivé par admin
    COURSE_UNARCHIVED_BY_ADMIN, // Instructor : cours désarchivé par admin
    COURSE_ARCHIVED_BY_INSTRUCTOR, // Admin/SuperAdmin : cours archivé par instructor
    COURSE_UNARCHIVED_BY_INSTRUCTOR, // Admin/SuperAdmin : cours désarchivé par instructor
    CHALLENGE_UNLOCKED,   // Étudiant : challenge débloqué
    NEW_MESSAGE           // Recruteur/Étudiant : nouveau message reçu
}

