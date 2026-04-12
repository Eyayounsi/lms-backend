package com.elearning.ProjetPfe.entity.engagement;

/**
 * Types de détection caméra pour le suivi d'attention.
 */
public enum DetectionRemarkType {
    // Negative
    EYES_CLOSED,
    YAWNING,
    LOOKING_AWAY,
    ABSENT,
    SAD,
    ANGRY,
    TIRED,
    // Positive
    SMILING,
    CONCENTRATED
}
