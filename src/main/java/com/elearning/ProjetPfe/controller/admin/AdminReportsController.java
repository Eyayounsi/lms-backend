package com.elearning.ProjetPfe.controller.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.entity.course.Course;
import com.elearning.ProjetPfe.entity.course.CourseStatus;
import com.elearning.ProjetPfe.entity.payment.Enrollment;
import com.elearning.ProjetPfe.entity.payment.PaymentStatus;
import com.elearning.ProjetPfe.entity.auth.Role;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.learning.CertificateRepository;
import com.elearning.ProjetPfe.repository.course.CourseRepository;
import com.elearning.ProjetPfe.repository.payment.EnrollmentRepository;
import com.elearning.ProjetPfe.repository.engagement.ReviewRepository;
import com.elearning.ProjetPfe.repository.admin.TicketRepository;
import com.elearning.ProjetPfe.repository.auth.UserRepository;

/**
 * Admin — rapports et analytics avancés.
 *
 * GET /api/admin/reports/overview          → stats globales
 * GET /api/admin/reports/revenue           → revenus par mois (12 derniers)
 * GET /api/admin/reports/courses           → top cours + stats par statut
 * GET /api/admin/reports/users             → inscriptions par mois
 * GET /api/admin/reports/enrollments       → inscriptions par mois
 */
@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportsController {

    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private CertificateRepository certificateRepository;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMM yyyy");
    private static final DateTimeFormatter KEY_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    /** Vue d'ensemble globale */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        Map<String, Object> data = new LinkedHashMap<>();

        long totalUsers = userRepository.count();
        long totalCourses = courseRepository.count();
        long publishedCourses = courseRepository.findByStatus(CourseStatus.PUBLISHED).size();
        List<Enrollment> paidEnrollments = enrollmentRepository.findAll().stream()
                .filter(e -> PaymentStatus.PAID.equals(e.getPaymentStatus()))
                .collect(Collectors.toList());
        long totalEnrollments = paidEnrollments.size();
        BigDecimal totalRevenue = paidEnrollments.stream()
                .map(e -> e.getCourse().getPrice() != null ? e.getCourse().getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalCertificates = certificateRepository.count();
        long openTickets = ticketRepository.findByStatusOrderByCreatedAtDesc("Opened").size();

        data.put("totalUsers", totalUsers);
        data.put("totalCourses", totalCourses);
        data.put("publishedCourses", publishedCourses);
        data.put("totalEnrollments", totalEnrollments);
        data.put("totalRevenue", totalRevenue);
        data.put("totalCertificates", totalCertificates);
        data.put("openTickets", openTickets);

        // This month stats
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long newUsersThisMonth = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(startOfMonth))
                .count();
        long newEnrollmentsThisMonth = paidEnrollments.stream()
                .filter(e -> e.getPaidAt() != null && e.getPaidAt().isAfter(startOfMonth))
                .count();
        data.put("newUsersThisMonth", newUsersThisMonth);
        data.put("newEnrollmentsThisMonth", newEnrollmentsThisMonth);

        return ResponseEntity.ok(data);
    }

    /** Revenus mensuels sur les 12 derniers mois */
    @GetMapping("/revenue")
    public ResponseEntity<Map<String, Object>> getRevenue() {
        List<Enrollment> paidEnrollments = enrollmentRepository.findAll().stream()
                .filter(e -> PaymentStatus.PAID.equals(e.getPaymentStatus()) && e.getPaidAt() != null)
                .collect(Collectors.toList());

        Map<String, BigDecimal> revenueByMonth = new LinkedHashMap<>();
        Map<String, Long> salesByMonth = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 11; i >= 0; i--) {
            LocalDateTime month = now.minusMonths(i);
            String key = month.format(MONTH_FMT);
            revenueByMonth.put(key, BigDecimal.ZERO);
            salesByMonth.put(key, 0L);
        }

        for (Enrollment e : paidEnrollments) {
            String key = e.getPaidAt().format(MONTH_FMT);
            if (revenueByMonth.containsKey(key)) {
                BigDecimal price = e.getCourse().getPrice() != null ? e.getCourse().getPrice() : BigDecimal.ZERO;
                revenueByMonth.merge(key, price, BigDecimal::add);
                salesByMonth.merge(key, 1L, Long::sum);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", new ArrayList<>(revenueByMonth.keySet()));
        result.put("revenue", new ArrayList<>(revenueByMonth.values()));
        result.put("sales", new ArrayList<>(salesByMonth.values()));
        return ResponseEntity.ok(result);
    }

    /** Stats des cours */
    @GetMapping("/courses")
    public ResponseEntity<Map<String, Object>> getCourseStats() {
        List<Course> allCourses = courseRepository.findAll();

        // Par statut
        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put("DRAFT", allCourses.stream().filter(c -> CourseStatus.DRAFT.equals(c.getStatus())).count());
        byStatus.put("PENDING", allCourses.stream().filter(c -> CourseStatus.PENDING.equals(c.getStatus())).count());
        byStatus.put("PUBLISHED", allCourses.stream().filter(c -> CourseStatus.PUBLISHED.equals(c.getStatus())).count());
        byStatus.put("REJECTED", allCourses.stream().filter(c -> CourseStatus.REJECTED.equals(c.getStatus())).count());

        // Top 10 cours par inscriptions
        List<Enrollment> paid = enrollmentRepository.findAll().stream()
                .filter(e -> PaymentStatus.PAID.equals(e.getPaymentStatus()))
                .collect(Collectors.toList());

        Map<Long, Long> enrollCountByCourse = paid.stream()
                .collect(Collectors.groupingBy(e -> e.getCourse().getId(), Collectors.counting()));

        List<Map<String, Object>> topCourses = enrollCountByCourse.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    courseRepository.findById(entry.getKey()).ifPresent(c -> {
                        item.put("courseId", c.getId());
                        item.put("title", c.getTitle());
                        item.put("enrollments", entry.getValue());
                        item.put("revenue", paid.stream()
                                .filter(e -> e.getCourse().getId().equals(c.getId()))
                                .map(e -> e.getCourse().getPrice() != null ? e.getCourse().getPrice() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));
                        item.put("status", c.getStatus().name());
                    });
                    return item;
                })
                .filter(m -> !m.isEmpty())
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("byStatus", byStatus);
        result.put("topCourses", topCourses);
        result.put("totalCourses", allCourses.size());
        return ResponseEntity.ok(result);
    }

    /** Inscriptions utilisateurs par mois */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        List<User> allUsers = userRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        Map<String, Long> byMonth = new LinkedHashMap<>();
        for (int i = 11; i >= 0; i--) {
            LocalDateTime month = now.minusMonths(i);
            String key = month.format(MONTH_FMT);
            int y = month.getYear(), m = month.getMonthValue();
            long count = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null
                            && u.getCreatedAt().getYear() == y
                            && u.getCreatedAt().getMonthValue() == m)
                    .count();
            byMonth.put(key, count);
        }

        // Par rôle
        Map<String, Long> byRole = new LinkedHashMap<>();
        for (Role r : Role.values()) {
            byRole.put(r.name(), allUsers.stream().filter(u -> u.getRole() == r).count());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", new ArrayList<>(byMonth.keySet()));
        result.put("registrations", new ArrayList<>(byMonth.values()));
        result.put("byRole", byRole);
        result.put("total", allUsers.size());
        return ResponseEntity.ok(result);
    }
}
