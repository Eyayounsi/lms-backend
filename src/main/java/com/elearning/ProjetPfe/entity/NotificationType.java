package com.elearning.ProjetPfe.entity;

/**
 * Types de notifications du système.
 */
public enum NotificationType {
    PURCHASE_CONFIRMED,   // Étudiant : achat confirmé
    COURSE_APPROVED,      // Instructor : cours approuvé par l'admin
    COURSE_REJECTED,      // Instructor : cours rejeté par l'admin
    CERTIFICATE_ISSUED,   // Étudiant : certificat généré
    NEW_REVENUE           // Instructor : nouveau revenu enregistré
}

