package com.elearning.ProjetPfe.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Toutes les notifications d'un utilisateur, triées par date DESC */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Nombre de notifications non lues */
    long countByUserIdAndReadFalse(Long userId);

    /** Marquer toutes les notifications comme lues pour un utilisateur */
    List<Notification> findByUserIdAndReadFalse(Long userId);
}

