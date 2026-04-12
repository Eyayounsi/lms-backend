package com.elearning.ProjetPfe.entity.engagement;

import com.elearning.ProjetPfe.entity.auth.User;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Un challenge assigné à un étudiant. 10 sont créés à l'inscription.
 * Chaque challenge a un code unique, un seuil à atteindre, et des points de récompense.
 */
@Entity
@Table(name = "challenges", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "challenge_code"})
})
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /** Code unique du challenge (ex: BUY_5_COURSES, COMPLETE_3_COURSES …) */
    @Enumerated(EnumType.STRING)
    @Column(name = "challenge_code", nullable = false, length = 50)
    private ChallengeCode challengeCode;

    /** Titre court affiché dans l'interface */
    @Column(nullable = false, length = 120)
    private String title;

    /** Description détaillée */
    @Column(length = 300)
    private String description;

    /** Icône CSS (ex: "ti ti-shopping-cart") */
    @Column(name = "icon_class", length = 60)
    private String iconClass;

    /** Nombre requis pour compléter (ex: 5 cours) */
    @Column(name = "target_count", nullable = false)
    private int targetCount;

    /** Progression actuelle de l'étudiant */
    @Column(name = "current_count", nullable = false)
    private int currentCount = 0;

    /** Points gagnés en complétant le challenge */
    @Column(name = "reward_points", nullable = false)
    private int rewardPoints;

    /** true = challenge débloqué et récompense attribuée */
    @Column(nullable = false)
    private boolean unlocked = false;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

    // ─── Getters & Setters ────────────────────────────────

    public Long getId() { return id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public ChallengeCode getChallengeCode() { return challengeCode; }
    public void setChallengeCode(ChallengeCode challengeCode) { this.challengeCode = challengeCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconClass() { return iconClass; }
    public void setIconClass(String iconClass) { this.iconClass = iconClass; }

    public int getTargetCount() { return targetCount; }
    public void setTargetCount(int targetCount) { this.targetCount = targetCount; }

    public int getCurrentCount() { return currentCount; }
    public void setCurrentCount(int currentCount) { this.currentCount = currentCount; }

    public int getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(int rewardPoints) { this.rewardPoints = rewardPoints; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public LocalDateTime getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(LocalDateTime unlockedAt) { this.unlockedAt = unlockedAt; }
}
