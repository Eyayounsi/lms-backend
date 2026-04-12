package com.elearning.ProjetPfe.controller.student;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.entity.engagement.Challenge;
import com.elearning.ProjetPfe.entity.payment.Coupon;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.auth.UserRepository;
import com.elearning.ProjetPfe.service.engagement.ChallengeService;

@RestController
@RequestMapping("/api/student/challenges")
public class StudentChallengeController {

    private final ChallengeService challengeService;
    private final UserRepository userRepository;

    public StudentChallengeController(ChallengeService challengeService,
                                      UserRepository userRepository) {
        this.challengeService = challengeService;
        this.userRepository = userRepository;
    }

    private User resolveUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

    /**
     * GET /api/student/challenges
     * Retourne les 10 challenges + progression, et init si première visite.
     */
    @GetMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> getChallenges(Authentication authentication) {

        User currentUser = resolveUser(authentication);

        // Initialiser si premier accès
        challengeService.initChallengesForStudent(currentUser);
        // Mettre à jour la progression
        challengeService.evaluateAll(currentUser);

        List<Challenge> challenges = challengeService.getStudentChallenges(currentUser.getId());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        List<Map<String, Object>> items = challenges.stream().map(ch -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", ch.getId());
            m.put("code", ch.getChallengeCode().name());
            m.put("title", ch.getTitle());
            m.put("description", ch.getDescription());
            m.put("iconClass", ch.getIconClass());
            m.put("targetCount", ch.getTargetCount());
            m.put("currentCount", ch.getCurrentCount());
            m.put("rewardPoints", ch.getRewardPoints());
            m.put("unlocked", ch.isUnlocked());
            m.put("unlockedAt", ch.getUnlockedAt() != null ? ch.getUnlockedAt().format(fmt) : null);
            m.put("progressPercent",
                    ch.getTargetCount() > 0 ? Math.min(100, (ch.getCurrentCount() * 100) / ch.getTargetCount()) : 0);
            return m;
        }).collect(Collectors.toList());

        long totalPoints = challenges.stream().filter(Challenge::isUnlocked).mapToInt(Challenge::getRewardPoints).sum();
        long unlockedCount = challenges.stream().filter(Challenge::isUnlocked).count();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("challenges", items);
        resp.put("points", currentUser.getChallengePoints());
        resp.put("totalEarnedPoints", totalPoints);
        resp.put("unlockedCount", unlockedCount);
        resp.put("totalCount", challenges.size());
        return ResponseEntity.ok(resp);
    }

    /**
     * POST /api/student/challenges/exchange
     * Échanger des points contre un coupon (3 paliers).
     * Body: { "tier": 1|2|3 }
     */
    @PostMapping("/exchange")
    @Transactional
    public ResponseEntity<Map<String, Object>> exchangePoints(
            @RequestBody Map<String, Integer> body,
            Authentication authentication) {
        User currentUser = resolveUser(authentication);
        try {
            int tier = body.getOrDefault("tier", 1);
            if (tier < 1 || tier > 3) {
                return ResponseEntity.badRequest().body(Map.of("error", "Palier invalide (1, 2 ou 3)"));
            }

            Coupon coupon = challengeService.exchangePoints(currentUser, tier);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("couponCode", coupon.getCouponCode());
            resp.put("discountPercent", coupon.getDiscountPercent());
            resp.put("pointsRemaining", currentUser.getChallengePoints());
            return ResponseEntity.ok(resp);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/student/challenges/coupons
     * Tous les coupons de l'étudiant.
     */
    @GetMapping("/coupons")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getMyCoupons(Authentication authentication) {
        User currentUser = resolveUser(authentication);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        List<Map<String, Object>> items = challengeService.getStudentCoupons(currentUser.getId())
                .stream().map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("couponCode", c.getCouponCode());
                    m.put("discountPercent", c.getDiscountPercent());
                    m.put("pointsSpent", c.getPointsSpent());
                    m.put("used", c.isUsed());
                    m.put("status", c.getStatus());
                    m.put("createdAt", c.getCreatedAt().format(fmt));
                    m.put("expiresAt", c.getExpiresAt() != null ? c.getExpiresAt().format(fmt) : null);
                    return m;
                }).collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    /**
     * POST /api/student/challenges/validate-coupon
     * Vérifier si un code coupon est valide (non expiré, non utilisé, appartient au student).
     * Body: { "couponCode": "CPE-XXXXXXXX" }
     */
    @PostMapping("/validate-coupon")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> validateCoupon(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        User currentUser = resolveUser(authentication);
        try {
            String couponCode = body.get("couponCode");
            if (couponCode == null || couponCode.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Code coupon requis"));
            }
            Coupon coupon = challengeService.validateCoupon(couponCode.trim(), currentUser.getId());
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("valid", true);
            resp.put("couponCode", coupon.getCouponCode());
            resp.put("discountPercent", coupon.getDiscountPercent());
            return ResponseEntity.ok(resp);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", e.getMessage()));
        }
    }
}
