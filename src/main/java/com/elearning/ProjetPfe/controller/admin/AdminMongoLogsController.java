package com.elearning.ProjetPfe.controller.admin;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.mongo.document.AiSessionLogDocument;
import com.elearning.ProjetPfe.mongo.document.AuthLogDocument;
import com.elearning.ProjetPfe.mongo.document.RecommendationLogDocument;
import com.elearning.ProjetPfe.service.mongo.MongoAuditService;

@RestController
@RequestMapping("/api/superadmin/mongo-logs")
public class AdminMongoLogsController {

    private final MongoAuditService mongoAuditService;

    public AdminMongoLogsController(MongoAuditService mongoAuditService) {
        this.mongoAuditService = mongoAuditService;
    }

    @GetMapping("/auth")
    public ResponseEntity<Page<AuthLogDocument>> getAuthLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String provider,
            @RequestParam(defaultValue = "") String email
    ) {
        return ResponseEntity.ok(
                mongoAuditService.getAuthLogs(safePage(page), safeSize(size), provider, email)
        );
    }

    @GetMapping("/recommendations")
    public ResponseEntity<Page<RecommendationLogDocument>> getRecommendationLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String type
    ) {
        return ResponseEntity.ok(
                mongoAuditService.getRecommendationLogs(safePage(page), safeSize(size), type)
        );
    }

    @GetMapping("/ai-sessions")
    public ResponseEntity<Page<AiSessionLogDocument>> getAiSessionLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String sessionId,
            @RequestParam(defaultValue = "") String userEmail
    ) {
        return ResponseEntity.ok(
                mongoAuditService.getAiSessionLogs(safePage(page), safeSize(size), sessionId, userEmail)
        );
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "message", "Mongo logs endpoint actif (si app.mongo.enabled=true)",
                "basePath", "/api/superadmin/mongo-logs"
        ));
    }

    private int safePage(int page) {
        return Math.max(page, 0);
    }

    private int safeSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}
