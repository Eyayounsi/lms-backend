package com.elearning.ProjetPfe.service.payment;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.entity.communication.NotificationType;
import com.elearning.ProjetPfe.entity.course.Course;
import com.elearning.ProjetPfe.entity.course.CourseStatus;
import com.elearning.ProjetPfe.entity.payment.Coupon;
import com.elearning.ProjetPfe.entity.payment.Enrollment;
import com.elearning.ProjetPfe.entity.payment.PaymentStatus;
import com.elearning.ProjetPfe.repository.course.CourseRepository;
import com.elearning.ProjetPfe.repository.payment.CouponRepository;
import com.elearning.ProjetPfe.repository.payment.EnrollmentRepository;
import com.elearning.ProjetPfe.service.communication.NotificationService;
import com.elearning.ProjetPfe.service.engagement.ChallengeService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.annotation.PostConstruct;

/**
 * Service de paiement Stripe.
 *
 * ═══════════════════════════════════════════════════
 * COMMENT ÇA MARCHE (simplement) :
 * ═══════════════════════════════════════════════════
 *
 * 1. createCheckoutSession() → crée une "session de paiement" chez Stripe
 *    C'est comme un ticket de caisse avec : le nom du cours, le prix, et l'URL de retour.
 *    Stripe nous retourne une URL vers sa propre page de paiement.
 *
 * 2. Le student est redirigé vers cette URL Stripe.
 *    Il voit un formulaire de carte bancaire (hébergé par Stripe, pas chez nous).
 *    En mode test, il utilise la carte : 4242 4242 4242 4242
 *
 * 3. handleWebhook() → Stripe nous NOTIFIE quand le paiement est confirmé.
 *    C'est un "callback" : Stripe envoie un POST à notre serveur.
 *    On vérifie la signature (sécurité) puis on met à jour la base de données.
 *
 * ═══════════════════════════════════════════════════
 * CARTES DE TEST STRIPE :
 * ═══════════════════════════════════════════════════
 *   ✅ Paiement réussi  : 4242 4242 4242 4242
 *   ❌ Paiement refusé  : 4000 0000 0000 0002
 *   ⚠️ Authentification : 4000 0025 0000 3155
 *   Date d'expiration   : n'importe quelle date future (ex: 12/34)
 *   CVC                 : n'importe quel nombre (ex: 123)
 */
@Service
public class PaymentService {

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CouponRepository couponRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private InstructorRevenueService instructorRevenueService;

    @Autowired
    private ChallengeService challengeService;

    public PaymentService(CourseRepository courseRepository,
                          EnrollmentRepository enrollmentRepository,
                          CouponRepository couponRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.couponRepository = couponRepository;
    }

    /**
     * @PostConstruct = cette méthode s'exécute au démarrage de l'application.
     * On initialise la clé secrète Stripe une seule fois.
     */
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CRÉER UNE SESSION DE PAIEMENT
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Crée une session de paiement Stripe (Checkout Session).
     *
     * @param courseId  l'ID du cours à acheter
     * @param student   l'utilisateur connecté
     * @return          Map avec l'URL Stripe et l'ID de la session
     */
    public Map<String, String> createCheckoutSession(Long courseId, User student, String couponCode)
            throws StripeException {

        // 1. Vérifier que le cours existe et est publié
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new RuntimeException("Ce cours n'est pas disponible à l'achat");
        }

        // 2. Vérifier que l'étudiant n'a pas déjà acheté ce cours
        enrollmentRepository.findByStudentIdAndCourseIdAndPaymentStatus(
                student.getId(), courseId, PaymentStatus.PAID
        ).ifPresent(e -> {
            throw new RuntimeException("Vous avez déjà acheté ce cours");
        });

        // 3. Valider le coupon si fourni
        Coupon coupon = null;
        if (couponCode != null && !couponCode.isBlank()) {
            coupon = challengeService.validateCoupon(couponCode, student.getId());
        }

        // 4. Calculer le prix avec réduction éventuelle
        // Utiliser le prix effectif pour respecter une promotion admin active.
        java.math.BigDecimal basePrice = course.getEffectivePrice();
        if (basePrice == null) {
            basePrice = course.getPrice();
        }
        if (coupon != null) {
            java.math.BigDecimal discount = basePrice
                    .multiply(java.math.BigDecimal.valueOf(coupon.getDiscountPercent()))
                    .divide(java.math.BigDecimal.valueOf(100));
            basePrice = basePrice.subtract(discount);
            if (basePrice.compareTo(java.math.BigDecimal.ZERO) < 0) {
                basePrice = java.math.BigDecimal.ONE; // Minimum 1 centime
            }
        }

        // 5. Créer la session Stripe Checkout
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .setCustomerEmail(student.getEmail())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setUnitAmount(basePrice
                                                        .multiply(java.math.BigDecimal.valueOf(100))
                                                        .longValue())
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(course.getTitle())
                                                                .setDescription(course.getDescription() != null
                                                                        ? course.getDescription().substring(0,
                                                                                Math.min(course.getDescription().length(), 200))
                                                                        : "Cours e-learning")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .putMetadata("course_id", courseId.toString())
                .putMetadata("student_id", student.getId().toString())
                .putMetadata("coupon_code", coupon != null ? coupon.getCouponCode() : "")
                .build();

        // 6. Envoyer la demande à Stripe
        Session session = Session.create(params);

        // 7. Créer un Enrollment en statut PENDING
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(student.getId(), courseId)
                .orElse(new Enrollment());

        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStripeSessionId(session.getId());
        enrollment.setPaymentStatus(PaymentStatus.PENDING);
        if (coupon != null) {
            enrollment.setCoupon(coupon);
        }
        enrollmentRepository.save(enrollment);

        // 8. Retourner l'URL de la page Stripe au frontend
        Map<String, String> response = new HashMap<>();
        response.put("sessionId", session.getId());
        response.put("url", session.getUrl());
        return response;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  WEBHOOK — Stripe nous notifie après le paiement
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Appelé automatiquement par Stripe quand le paiement est terminé.
     *
     * Stripe envoie un POST avec un "Event" signé.
     * On vérifie la signature pour être sûr que c'est bien Stripe (pas un pirate).
     * Puis on met à jour l'Enrollment en base : PENDING → PAID.
     *
     * @param payload   le corps brut de la requête Stripe
     * @param sigHeader la signature Stripe dans le header
     */
    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        Event event;

        // 1. Vérifier la signature — sécurité critique
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new RuntimeException("Signature webhook invalide");
        } catch (Exception e) {
            throw new RuntimeException("Erreur parsing webhook: " + e.getMessage());
        }

        // 2. Traiter l'événement "checkout.session.completed"
        // C'est l'événement envoyé quand le paiement est confirmé
        if ("checkout.session.completed".equals(event.getType())) {

            // Récupérer la session à partir de l'événement
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject().orElse(null);

            if (session != null) {
                // Trouver l'enrollment par session Stripe
                enrollmentRepository.findByStripeSessionId(session.getId())
                        .ifPresent(enrollment -> {
                            enrollment.setPaymentStatus(PaymentStatus.PAID);
                            enrollment.setPaidAt(LocalDateTime.now());
                            enrollmentRepository.save(enrollment);
                            System.out.println("✅ Paiement confirmé pour session: " + session.getId());

                            // Marquer le coupon comme utilisé
                            if (enrollment.getCoupon() != null) {
                                Coupon usedCoupon = enrollment.getCoupon();
                                usedCoupon.setUsed(true);
                                couponRepository.save(usedCoupon);
                            }

                            // Notification étudiant
                            notificationService.send(
                                    enrollment.getStudent(),
                                    NotificationType.PURCHASE_CONFIRMED,
                                    "🎉 Achat confirmé !",
                                    "Votre achat du cours \"" + enrollment.getCourse().getTitle() + "\" a été confirmé.",
                                    "/student/my-courses"
                            );

                            // Enregistrement du revenu instructor
                            instructorRevenueService.recordSale(enrollment);
                        });
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  VÉRIFIER SI UN ÉTUDIANT A PAYÉ UN COURS
    // ═══════════════════════════════════════════════════════════════════════

    public boolean hasStudentPaidCourse(Long studentId, Long courseId) {
        return enrollmentRepository
                .findByStudentIdAndCourseIdAndPaymentStatus(studentId, courseId, PaymentStatus.PAID)
                .isPresent();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CONFIRMER LE PAIEMENT par session_id (fallback sans webhook)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Appelé par le frontend après retour de Stripe (success URL).
     * Vérifie directement auprès de l'API Stripe si la session est payée
     * et met à jour l'enrollment PENDING → PAID.
     * C'est le fallback pour les environnements locaux sans webhook.
     */
    @Transactional
    public void confirmSessionPayment(String sessionId, Long authenticatedStudentId) throws StripeException {
        // Récupérer la session Stripe (appel direct à l'API Stripe)
        Session session = Session.retrieve(sessionId);

        if ("paid".equals(session.getPaymentStatus())) {
            enrollmentRepository.findByStripeSessionId(sessionId)
                    .ifPresent(enrollment -> {
                        if (!enrollment.getStudent().getId().equals(authenticatedStudentId)) {
                            throw new RuntimeException("Session Stripe non autorisée pour cet utilisateur");
                        }

                        if (enrollment.getPaymentStatus() != PaymentStatus.PAID) {
                            enrollment.setPaymentStatus(PaymentStatus.PAID);
                            enrollment.setPaidAt(LocalDateTime.now());
                            enrollmentRepository.save(enrollment);
                            System.out.println("✅ Paiement confirmé (fallback) pour session: " + sessionId);

                            // Marquer le coupon comme utilisé
                            if (enrollment.getCoupon() != null) {
                                Coupon usedCoupon = enrollment.getCoupon();
                                usedCoupon.setUsed(true);
                                couponRepository.save(usedCoupon);
                            }

                            // Notification étudiant
                            notificationService.send(
                                    enrollment.getStudent(),
                                    NotificationType.PURCHASE_CONFIRMED,
                                    "🎉 Achat confirmé !",
                                    "Votre achat du cours \"" + enrollment.getCourse().getTitle() + "\" a été confirmé.",
                                    "/student/my-courses"
                            );

                            // Enregistrement du revenu instructor
                            instructorRevenueService.recordSale(enrollment);
                        }
                    });
        }
    }
}
