package com.elearning.ProjetPfe.service.engagement;

import com.elearning.ProjetPfe.service.communication.NotificationService;
import com.elearning.ProjetPfe.entity.communication.Notification;
import com.elearning.ProjetPfe.entity.communication.Message;
import com.elearning.ProjetPfe.entity.learning.Question;
import com.elearning.ProjetPfe.entity.learning.Quiz;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elearning.ProjetPfe.entity.engagement.Challenge;
import com.elearning.ProjetPfe.entity.engagement.ChallengeCode;
import com.elearning.ProjetPfe.entity.payment.Coupon;
import com.elearning.ProjetPfe.entity.communication.NotificationType;
import com.elearning.ProjetPfe.entity.payment.PaymentStatus;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.learning.CertificateRepository;
import com.elearning.ProjetPfe.repository.engagement.ChallengeRepository;
import com.elearning.ProjetPfe.repository.payment.CouponRepository;
import com.elearning.ProjetPfe.repository.communication.CourseAnswerRepository;
import com.elearning.ProjetPfe.repository.learning.CourseProgressRepository;
import com.elearning.ProjetPfe.repository.communication.CourseQuestionRepository;
import com.elearning.ProjetPfe.repository.payment.EnrollmentRepository;
import com.elearning.ProjetPfe.repository.learning.LessonProgressRepository;
import com.elearning.ProjetPfe.repository.learning.QuizAttemptRepository;
import com.elearning.ProjetPfe.repository.engagement.ReviewRepository;
import com.elearning.ProjetPfe.repository.auth.UserRepository;

@Service
public class ChallengeService {

    private final ChallengeRepository challengeRepo;
    private final CouponRepository couponRepo;
    private final UserRepository userRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final CourseProgressRepository progressRepo;
    private final CertificateRepository certRepo;
    private final QuizAttemptRepository quizAttemptRepo;
    private final ReviewRepository reviewRepo;
    private final LessonProgressRepository lessonProgressRepo;
    private final CourseQuestionRepository questionRepo;
    private final CourseAnswerRepository answerRepo;
    private final NotificationService notificationService;

    public ChallengeService(ChallengeRepository challengeRepo,
                            CouponRepository couponRepo,
                            UserRepository userRepo,
                            EnrollmentRepository enrollmentRepo,
                            CourseProgressRepository progressRepo,
                            CertificateRepository certRepo,
                            QuizAttemptRepository quizAttemptRepo,
                            ReviewRepository reviewRepo,
                            LessonProgressRepository lessonProgressRepo,
                            CourseQuestionRepository questionRepo,
                            CourseAnswerRepository answerRepo,
                            NotificationService notificationService) {
        this.challengeRepo = challengeRepo;
        this.couponRepo = couponRepo;
        this.userRepo = userRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.progressRepo = progressRepo;
        this.certRepo = certRepo;
        this.quizAttemptRepo = quizAttemptRepo;
        this.reviewRepo = reviewRepo;
        this.lessonProgressRepo = lessonProgressRepo;
        this.questionRepo = questionRepo;
        this.answerRepo = answerRepo;
        this.notificationService = notificationService;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Initialiser les 10 challenges pour un nouvel étudiant
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public void initChallengesForStudent(User student) {
        if (challengeRepo.existsByStudentId(student.getId())) return;

        createChallenge(student, ChallengeCode.FIRST_PURCHASE,
                "Premier pas", "Achetez votre premier cours",
                "ti ti-rocket", 1, 15);

        createChallenge(student, ChallengeCode.FIRST_LESSON,
                "Première leçon", "Complétez votre première leçon",
                "ti ti-player-play", 1, 10);

        createChallenge(student, ChallengeCode.COMPLETE_10_LESSONS,
                "Apprenti assidu", "Complétez 10 leçons",
                "ti ti-book", 10, 30);

        createChallenge(student, ChallengeCode.COMPLETE_25_LESSONS,
                "Expert des leçons", "Complétez 25 leçons",
                "ti ti-books", 25, 50);

        createChallenge(student, ChallengeCode.FINISH_FIRST_COURSE,
                "Première victoire", "Terminez un cours à 100%",
                "ti ti-circle-check", 1, 40);

        createChallenge(student, ChallengeCode.FINISH_3_COURSES,
                "Maître apprenant", "Terminez 3 cours à 100%",
                "ti ti-crown", 3, 60);

        createChallenge(student, ChallengeCode.BUY_5_COURSES,
                "Collectionneur", "Achetez 5 cours",
                "ti ti-shopping-cart", 5, 50);

        createChallenge(student, ChallengeCode.PASS_5_QUIZZES,
                "Quiz Master", "Réussissez 5 quiz",
                "ti ti-brain", 5, 30);

        createChallenge(student, ChallengeCode.ASK_3_QUESTIONS,
                "Curieux", "Posez 3 questions dans le Q&A",
                "ti ti-message-question", 3, 20);

        createChallenge(student, ChallengeCode.COMPLETE_PROFILE,
                "Profil complet", "Remplissez toutes les infos de votre profil",
                "ti ti-user-check", 1, 15);
    }

    private void createChallenge(User student, ChallengeCode code,
                                 String title, String desc,
                                 String icon, int target, int reward) {
        Challenge c = new Challenge();
        c.setStudent(student);
        c.setChallengeCode(code);
        c.setTitle(title);
        c.setDescription(desc);
        c.setIconClass(icon);
        c.setTargetCount(target);
        c.setRewardPoints(reward);
        challengeRepo.save(c);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Récupérer les challenges d'un étudiant
    // ══════════════════════════════════════════════════════════════════════

    public List<Challenge> getStudentChallenges(Long studentId) {
        return challengeRepo.findByStudentIdOrderByChallengeCodeAsc(studentId);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Évaluer et mettre à jour la progression de TOUS les challenges
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public void evaluateAll(User student) {
        Long sid = student.getId();

        // Compter les métriques actuelles
        long paidEnrollments = enrollmentRepo
                .findByStudentIdAndPaymentStatus(sid, PaymentStatus.PAID).size();
        long completedCourses = progressRepo.countCompletedByStudentId(sid);
        long completedLessons = lessonProgressRepo.countByStudentIdAndCompletedTrue(sid);
        long passedQuizzes = quizAttemptRepo.countDistinctPassedByStudentId(sid);
        long questionsAsked = questionRepo.countByStudentId(sid);
        boolean profileComplete = isProfileComplete(student);

        List<Challenge> challenges = challengeRepo.findByStudentIdOrderByChallengeCodeAsc(sid);

        for (Challenge ch : challenges) {
            if (ch.isUnlocked()) continue;

            int newCount = switch (ch.getChallengeCode()) {
                case FIRST_PURCHASE -> (int) Math.min(paidEnrollments, ch.getTargetCount());
                case FIRST_LESSON -> (int) Math.min(completedLessons, ch.getTargetCount());
                case COMPLETE_10_LESSONS, COMPLETE_25_LESSONS -> (int) Math.min(completedLessons, ch.getTargetCount());
                case FINISH_FIRST_COURSE, FINISH_3_COURSES -> (int) Math.min(completedCourses, ch.getTargetCount());
                case BUY_5_COURSES -> (int) Math.min(paidEnrollments, ch.getTargetCount());
                case PASS_5_QUIZZES -> (int) Math.min(passedQuizzes, ch.getTargetCount());
                case ASK_3_QUESTIONS -> (int) Math.min(questionsAsked, ch.getTargetCount());
                case COMPLETE_PROFILE -> profileComplete ? 1 : 0;
            };

            ch.setCurrentCount(newCount);

            if (newCount >= ch.getTargetCount()) {
                ch.setUnlocked(true);
                ch.setUnlockedAt(LocalDateTime.now());
                student.setChallengePoints(student.getChallengePoints() + ch.getRewardPoints());
                userRepo.save(student);

                try {
                    notificationService.send(student,
                            NotificationType.CHALLENGE_UNLOCKED,
                            "Challenge débloqué : " + ch.getTitle(),
                            "Félicitations ! Vous avez gagné " + ch.getRewardPoints() + " points !",
                            "/student/student-challenges");
                } catch (Exception ignored) {
                    // La notification est non-bloquante : un échec ne doit pas empêcher le déblocage du challenge
                }
            }

            challengeRepo.save(ch);
        }
    }

    private boolean isProfileComplete(User u) {
        return u.getFullName() != null && !u.getFullName().isBlank()
                && u.getPhone() != null && !u.getPhone().isBlank()
                && u.getBio() != null && !u.getBio().isBlank()
                && u.getAvatarPath() != null && !u.getAvatarPath().isBlank();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Échanger des points contre un coupon (3 paliers)
    // ══════════════════════════════════════════════════════════════════════

    /** Paliers : tier 1 = 100pts→10%, tier 2 = 200pts→20%, tier 3 = 300pts→30% */
    public static int getPointsForTier(int tier) {
        return switch (tier) {
            case 1 -> 100;
            case 2 -> 200;
            case 3 -> 300;
            default -> throw new IllegalArgumentException("Palier invalide : " + tier);
        };
    }

    public static int getDiscountForTier(int tier) {
        return switch (tier) {
            case 1 -> 10;
            case 2 -> 20;
            case 3 -> 30;
            default -> throw new IllegalArgumentException("Palier invalide : " + tier);
        };
    }

    @Transactional
    public Coupon exchangePoints(User student, int tier) {
        int requiredPoints = getPointsForTier(tier);
        int discountPercent = getDiscountForTier(tier);

        if (student.getChallengePoints() < requiredPoints) {
            throw new IllegalStateException("Points insuffisants (minimum " + requiredPoints + " pour ce palier)");
        }

        student.setChallengePoints(student.getChallengePoints() - requiredPoints);
        userRepo.save(student);

        Coupon coupon = new Coupon();
        coupon.setStudent(student);
        coupon.setCouponCode("CPE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        coupon.setPointsSpent(requiredPoints);
        coupon.setDiscountPercent(discountPercent);
        return couponRepo.save(coupon);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Valider un coupon pour un paiement
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Valide un coupon : existe, appartient au student, pas utilisé, pas expiré.
     */
    public Coupon validateCoupon(String couponCode, Long studentId) {
        Coupon coupon = couponRepo.findByCouponCode(couponCode)
                .orElseThrow(() -> new IllegalStateException("Code coupon introuvable"));

        if (!coupon.getStudent().getId().equals(studentId)) {
            throw new IllegalStateException("Ce coupon ne vous appartient pas");
        }
        if (coupon.isUsed()) {
            throw new IllegalStateException("Ce coupon a déjà été utilisé");
        }
        if (coupon.isExpired()) {
            throw new IllegalStateException("Ce coupon a expiré");
        }
        return coupon;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Lister les coupons d'un étudiant
    // ══════════════════════════════════════════════════════════════════════

    public List<Coupon> getStudentCoupons(Long studentId) {
        return couponRepo.findByStudentIdOrderByCreatedAtDesc(studentId);
    }
}
