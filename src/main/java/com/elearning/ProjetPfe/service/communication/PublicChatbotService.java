package com.elearning.ProjetPfe.service.communication;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.elearning.ProjetPfe.dto.chatbot.AiAgentRequest;
import com.elearning.ProjetPfe.dto.chatbot.AiAgentResponse;
import com.elearning.ProjetPfe.entity.course.Course;
import com.elearning.ProjetPfe.entity.course.CourseStatus;
import com.elearning.ProjetPfe.repository.course.CourseRepository;
import com.elearning.ProjetPfe.service.mongo.MongoAuditService;

@Service
public class PublicChatbotService {

    private final RestTemplate restTemplate;
    private final MongoAuditService mongoAuditService;
    private final CourseRepository courseRepository;

    @Value("${app.chatbot.hf.api-url:https://api-inference.huggingface.co/models}")
    private String huggingFaceApiUrl;

    @Value("${app.chatbot.hf.model:google/flan-t5-base}")
    private String huggingFaceModel;

    @Value("${app.chatbot.hf.token:}")
    private String huggingFaceToken;

    @Value("${app.chatbot.python-service.url:}")
    private String pythonChatbotServiceUrl;

    @Value("${app.chatbot.python-service.base-url:http://localhost:8090}")
    private String pythonChatbotServiceBaseUrl;

    @Value("${app.chatbot.python-service.agent-api-key:}")
    private String pythonAgentApiKey;

    @Value("${app.chatbot.max-new-tokens:220}")
    private Integer maxNewTokens;

    @Value("${app.chatbot.temperature:0.4}")
    private Double temperature;

    @Value("${app.chatbot.top-p:0.9}")
    private Double topP;

    public PublicChatbotService(RestTemplateBuilder restTemplateBuilder,
                                MongoAuditService mongoAuditService,
                                CourseRepository courseRepository) {
        this.restTemplate = restTemplateBuilder.build();
        this.mongoAuditService = mongoAuditService;
        this.courseRepository = courseRepository;
    }

    public String generateReply(String rawMessage) {
        String message = rawMessage == null ? "" : rawMessage.trim();
        if (message.isEmpty()) {
            return "Je suis là pour vous aider. Posez-moi votre question sur la plateforme.";
        }

        String businessReply = buildVisitorBusinessReply(message);
        if (businessReply != null && !businessReply.isBlank()) {
            return businessReply;
        }

        AiAgentRequest visitorRequest = new AiAgentRequest();
        visitorRequest.setMessage(message);
        AiAgentResponse visitorResponse = visitorSupport(visitorRequest);
        if (visitorResponse != null && visitorResponse.getReply() != null && !visitorResponse.getReply().isBlank()) {
            return visitorResponse.getReply();
        }

        String pythonReply = callPythonService(message);
        if (pythonReply != null && !pythonReply.isBlank()) {
            return pythonReply;
        }

        if (huggingFaceToken == null || huggingFaceToken.isBlank()) {
            return fallbackReply(message);
        }

        String prompt = buildPrompt(message);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("max_new_tokens", maxNewTokens);
        parameters.put("temperature", temperature);
        parameters.put("top_p", topP);
        parameters.put("return_full_text", false);

        Map<String, Object> options = new HashMap<>();
        options.put("wait_for_model", true);

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", prompt);
        body.put("parameters", parameters);
        body.put("options", options);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(huggingFaceToken.trim());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        String endpoint = huggingFaceApiUrl + "/" + huggingFaceModel;

        try {
            ResponseEntity<Object> response = restTemplate.exchange(endpoint, HttpMethod.POST, request, Object.class);
            String parsed = extractModelReply(response.getBody());
            if (parsed == null || parsed.isBlank()) {
                return fallbackReply(message);
            }
            return parsed;
        } catch (RestClientException ex) {
            return fallbackReply(message);
        }
    }

    public AiAgentResponse visitorSupport(AiAgentRequest request) {
        return callPythonAgent("/agents/visitor/support", request, true);
    }

    public AiAgentResponse studentTutor(AiAgentRequest request) {
        return callPythonAgent("/agents/student/tutor", request, true);
    }

    public AiAgentResponse instructorQuiz(AiAgentRequest request) {
        return callPythonAgent("/agents/instructor/quiz", request, true);
    }

    public AiAgentResponse instructorPlan(AiAgentRequest request) {
        return callPythonAgent("/agents/instructor/plan", request, true);
    }

    public AiAgentResponse instructorCopilot(AiAgentRequest request) {
        AiAgentResponse result = callPythonAgent("/agents/instructor/copilot", request, false);
        if (result == null || result.getReply() == null || result.getReply().isBlank()) {
            return fallbackAgentResponse(instructorCopilotFallback(request), "spring-fallback", true);
        }
        return result;
    }

    private String instructorCopilotFallback(AiAgentRequest request) {
        Map<String, Object> ctx = request != null ? request.getContext() : null;
        String target = ctx != null ? String.valueOf(ctx.getOrDefault("target", "")) : "";
        String summary = ctx != null ? String.valueOf(ctx.getOrDefault("instructorCatalogSummary", "")) : "";

        if ("audit".equals(target)) {
            String catalogLine = summary.isBlank() ? "Catalogue instructeur non disponible." : summary;
            return "## Diagnostic ciblé\n"
                + "- Contexte analysé: " + catalogLine + "\n"
                + "- Risque principal: structure cours/quiz potentiellement déséquilibrée.\n\n"
                + "## Écarts détectés (Top 5)\n"
                + "1. Alignement objectifs ↔ évaluations à renforcer.\n"
                + "2. Progression pédagogique trop abrupte entre modules.\n"
                + "3. Répartition types de contenus à équilibrer (article/vidéo/TP).\n"
                + "4. Densité quiz et feedback formatif insuffisante.\n"
                + "5. KPIs de pilotage non formalisés avec seuils d'alerte.\n\n"
                + "## Plan d'action priorisé (7/30/60 jours)\n"
                + "- J+7: audit module par module + matrice objectifs/évaluations.\n"
                + "- J+30: refonte des quizzes clés et ajout de feedback ciblé.\n"
                + "- J+60: optimisation des parcours et remédiation automatisée.\n\n"
                + "## Correctifs quiz & contenu\n"
                + "- Ajouter un quiz diagnostic en début de parcours.\n"
                + "- Ajouter mini-évaluations formatives par section.\n"
                + "- Standardiser un template de leçon (objectif → contenu → pratique → validation).\n\n"
                + "## KPI de suivi et seuils d'alerte\n"
                + "- Taux de complétion < 65% → simplifier le module.\n"
                + "- Score quiz moyen < 60% → renforcer les prérequis.\n"
                + "- Taux d'abandon > 25% → réduire la charge hebdomadaire.\n\n"
                + "## Prochaine action immédiate\n"
                + "Lancer un audit du cours le plus actif cette semaine et corriger 1 module + 1 quiz en priorité.";
        }

        return "Je suis votre **Instructor Copilot**.\n\n"
            + "## Comment je peux vous aider\n"
            + "- Construire un plan de cours structuré\n"
            + "- Générer des quiz pédagogiques\n"
            + "- Analyser et améliorer vos cours existants\n"
            + "- Conseils sur les meilleures pratiques pédagogiques\n\n"
            + "## Prochaine étape\n"
            + "Décrivez votre besoin dans le champ texte et cliquez sur **Générer**.";
    }

    public AiAgentResponse adminCopilot(AiAgentRequest request) {
        return callPythonAgent("/agents/admin/copilot", request, true);
    }

    @SuppressWarnings("unchecked")
    private AiAgentResponse callPythonAgent(String path, AiAgentRequest requestBody, boolean enableFallback) {
        String baseUrl = pythonChatbotServiceBaseUrl == null ? "" : pythonChatbotServiceBaseUrl.trim();
        if (baseUrl.isEmpty()) {
            AiAgentResponse response = fallbackAgentResponse(fallbackReply(requestBody != null ? requestBody.getMessage() : ""), "spring-fallback", true);
            safeLogAiSession(path, requestBody, response);
            return response;
        }

        String endpoint = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) + path : baseUrl + path;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (pythonAgentApiKey != null && !pythonAgentApiKey.isBlank()) {
                headers.add("X-Agent-Api-Key", pythonAgentApiKey.trim());
            }

            HttpEntity<AiAgentRequest> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, request, Map.class);

            Map responseBody = response.getBody();
            if (responseBody == null) {
                if (enableFallback) {
                    AiAgentResponse fallback = fallbackAgentResponse(fallbackReply(requestBody != null ? requestBody.getMessage() : ""), "spring-fallback", true);
                    safeLogAiSession(path, requestBody, fallback);
                    return fallback;
                }
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) responseBody;
            AiAgentResponse result = new AiAgentResponse();

            Object replyObj = body.get("reply");
            result.setReply(replyObj instanceof String ? normalizeReply((String) replyObj) : "");

            Object providerObj = body.get("provider");
            result.setProvider(providerObj instanceof String ? (String) providerObj : "python-agent");

            Object fallbackObj = body.get("fallback_used");
            if (fallbackObj instanceof Boolean b) {
                result.setFallbackUsed(b);
            } else {
                result.setFallbackUsed(false);
            }

            Object latencyObj = body.get("latency_ms");
            if (latencyObj instanceof Number n) {
                result.setLatencyMs(n.intValue());
            } else {
                result.setLatencyMs(0);
            }

            if ((result.getReply() == null || result.getReply().isBlank()) && enableFallback) {
                AiAgentResponse fallback = fallbackAgentResponse(fallbackReply(requestBody != null ? requestBody.getMessage() : ""), "spring-fallback", true);
                safeLogAiSession(path, requestBody, fallback);
                return fallback;
            }

            safeLogAiSession(path, requestBody, result);
            return result;
        } catch (RestClientException ex) {
            if (enableFallback) {
                AiAgentResponse fallback = fallbackAgentResponse(fallbackReply(requestBody != null ? requestBody.getMessage() : ""), "spring-fallback", true);
                safeLogAiSession(path, requestBody, fallback);
                return fallback;
            }
            return null;
        }
    }

    private void safeLogAiSession(String agentPath, AiAgentRequest requestBody, AiAgentResponse response) {
        try {
            String sessionId = requestBody != null ? requestBody.getSessionId() : null;
            if (sessionId == null || sessionId.isBlank()) {
                sessionId = "missing-session";
            }

            String userIdentifier = requestBody != null ? requestBody.getUserId() : null;
            if (userIdentifier == null || userIdentifier.isBlank()) {
                userIdentifier = "anonymous";
            }

            String prompt = requestBody != null ? requestBody.getMessage() : "";
            String model = response != null && response.getProvider() != null && !response.getProvider().isBlank()
                    ? response.getProvider()
                    : agentPath;
            String reply = response != null ? response.getReply() : "";
            Integer latencyMsValue = response != null ? response.getLatencyMs() : null;
            int latency = latencyMsValue != null ? latencyMsValue : 0;
            boolean success = reply != null && !reply.isBlank();

            mongoAuditService.logAiSession(sessionId, userIdentifier, model, prompt, reply, latency, success);
        } catch (Exception ignored) {
        }
    }

    private AiAgentResponse fallbackAgentResponse(String reply, String provider, boolean fallbackUsed) {
        AiAgentResponse response = new AiAgentResponse();
        response.setReply(reply);
        response.setProvider(provider);
        response.setFallbackUsed(fallbackUsed);
        response.setLatencyMs(0);
        return response;
    }

    @SuppressWarnings("unchecked")
    private String callPythonService(String message) {
        if (pythonChatbotServiceUrl == null || pythonChatbotServiceUrl.isBlank()) {
            return null;
        }

        try {
            Map<String, String> body = new HashMap<>();
            body.put("message", message);

            ResponseEntity<Map> response = restTemplate.postForEntity(pythonChatbotServiceUrl, body, Map.class);
            Map responseBody = response.getBody();
            if (responseBody == null) {
                return null;
            }

            Object reply = responseBody.get("reply");
            if (reply instanceof String text) {
                return normalizeReply(text);
            }
            return null;
        } catch (RestClientException ex) {
            return null;
        }
    }

    private String buildPrompt(String userMessage) {
        return "Tu es l'assistant virtuel d'une plateforme e-learning commerciale. "
                + "Réponds en français de manière concise, utile, polie et orientée conversion. "
                + "Si la question concerne un accès privé (paiement, espace personnel, progression), "
                + "invite à se connecter ou créer un compte. Question utilisateur: " + userMessage;
    }

    @SuppressWarnings("unchecked")
    private String extractModelReply(Object payload) {
        if (payload == null) {
            return "";
        }

        if (payload instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> map) {
                Object generated = map.get("generated_text");
                if (generated instanceof String text) {
                    return normalizeReply(text);
                }
                Object summary = map.get("summary_text");
                if (summary instanceof String text) {
                    return normalizeReply(text);
                }
            }
        }

        if (payload instanceof Map<?, ?> map) {
            Object generated = map.get("generated_text");
            if (generated instanceof String text) {
                return normalizeReply(text);
            }
            Object error = map.get("error");
            if (error instanceof String) {
                return "";
            }
        }

        return "";
    }

    private String normalizeReply(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.startsWith("Assistant:")) {
            normalized = normalized.substring("Assistant:".length()).trim();
        }
        return normalized;
    }

    private String fallbackReply(String message) {
        String m = message.toLowerCase();
        if (m.contains("prix") || m.contains("tarif") || m.contains("payer") || m.contains("achat")) {
            return "Nos offres et tarifs sont visibles dans le catalogue. Je peux vous guider vers le bon cours selon votre objectif.";
        }
        if (m.contains("certificat") || m.contains("certification")) {
            return "Oui, des certificats sont disponibles sur les parcours éligibles. Vous pouvez aussi vérifier un certificat depuis la page dédiée.";
        }
        if (m.contains("inscription") || m.contains("compte") || m.contains("login") || m.contains("connexion")) {
            return "Vous pouvez créer un compte en quelques secondes depuis la page d'inscription pour accéder à vos cours, certificats et progression.";
        }
        return "Je peux vous aider à trouver un cours, comprendre les fonctionnalités et démarrer rapidement. Quelle est votre priorité ?";
    }

    private String buildVisitorBusinessReply(String message) {
        String normalized = message == null ? "" : message.toLowerCase();

        if (containsAny(normalized, "acheter", "achat", "payer", "paiement", "stripe", "checkout", "carte", "prix", "tarif")) {
            return "Pour acheter un cours sur la plateforme, le paiement se fait **uniquement via Stripe**.\n\n"
                + "Étapes rapides :\n"
                + "1. Ouvrez un cours dans le catalogue\n"
                + "2. Cliquez sur **Acheter maintenant**\n"
                + "3. Vous serez redirigé vers la page sécurisée Stripe\n"
                + "4. Après paiement validé, le cours est ajouté à vos cours\n\n"
                + "Moyens de paiement pris en charge : carte bancaire via Stripe.";
        }

        if (containsAny(normalized, "recommande", "recommand", "quel cours", "cours pour", "debut", "début", "python", "java", "javascript", "web", "data", "ia")) {
            return buildCourseRecommendationsReply(message);
        }

        if (containsAny(normalized, "certificat", "certification", "attestation")) {
            return "Oui, les cours éligibles permettent d'obtenir un certificat après complétion.\n"
                + "Vous pouvez le consulter depuis votre espace utilisateur une fois le cours terminé.";
        }

        if (containsAny(normalized, "inscription", "compte", "connexion", "login", "register")) {
            return "Pour accéder aux cours achetés, quiz et certificats, créez un compte puis connectez-vous.\n"
                + "Ensuite vous pourrez gérer vos achats et votre progression directement depuis votre espace.";
        }

        return null;
    }

    private String buildCourseRecommendationsReply(String userMessage) {
        List<Course> publishedCourses = courseRepository.findByStatus(CourseStatus.PUBLISHED);
        if (publishedCourses == null || publishedCourses.isEmpty()) {
            return "Je n'ai pas encore de cours publiés à recommander pour le moment."
                + " Revenez bientôt sur le catalogue.";
        }

        Set<String> keywords = extractKeywords(userMessage);

        List<Course> ranked = publishedCourses.stream()
            .sorted((a, b) -> Integer.compare(scoreCourse(b, keywords), scoreCourse(a, keywords)))
            .collect(Collectors.toList());

        List<Course> top = ranked.stream().limit(3).toList();

        StringBuilder sb = new StringBuilder();
        sb.append("Voici des cours disponibles sur la plateforme que je vous recommande :\n\n");

        int idx = 1;
        for (Course course : top) {
            String categoryName = course.getCategory() != null ? course.getCategory().getName() : "Catégorie générale";
            String level = formatLevel(course);
            String price = course.getEffectivePrice() != null ? course.getEffectivePrice().toPlainString() : "0";

            sb.append(idx++)
                .append(". ")
                .append(course.getTitle())
                .append(" — ")
                .append(categoryName)
                .append(" — ")
                .append(level)
                .append(" — ")
                .append(price)
                .append(" TND\n");
        }

        sb.append("\nSi vous voulez, je peux affiner selon votre objectif (débutant, reconversion, data, web, IA). ");
        return sb.toString();
    }

    private String formatLevel(Course course) {
        if (course == null || course.getLevel() == null) {
            return "Niveau non précisé";
        }

        return switch (course.getLevel()) {
            case BEGINNER -> "Débutant";
            case INTERMEDIATE -> "Intermédiaire";
            case ADVANCED -> "Avancé";
        };
    }

    private int scoreCourse(Course course, Set<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return course.isFeatured() ? 2 : 0;
        }

        String title = safeLower(course.getTitle());
        String description = safeLower(course.getDescription());
        String category = course.getCategory() != null ? safeLower(course.getCategory().getName()) : "";
        String level = course.getLevel() != null ? safeLower(course.getLevel().name()) : "";

        int score = course.isFeatured() ? 2 : 0;

        for (String keyword : keywords) {
            if (title.contains(keyword)) {
                score += 5;
            }
            if (description.contains(keyword)) {
                score += 2;
            }
            if (category.contains(keyword)) {
                score += 3;
            }
            if (level.contains(keyword)) {
                score += 1;
            }
        }
        return score;
    }

    private Set<String> extractKeywords(String message) {
        Set<String> stopWordSet = Set.of(
            "quel", "quelle", "quels", "quelles", "pour", "avec", "dans", "vous", "nous", "cours",
            "des", "les", "une", "sur", "est", "sont", "aux", "par", "plus", "tres", "très"
        );

        Set<String> keywords = new HashSet<>();
        String normalized = message == null ? "" : message.toLowerCase().replaceAll("[^a-z0-9àâäéèêëîïôöùûüç ]", " ");
        for (String token : normalized.split("\\s+")) {
            if (token.length() < 3 || stopWordSet.contains(token)) {
                continue;
            }
            keywords.add(token);
        }
        return keywords;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private boolean containsAny(String value, String... probes) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (String probe : probes) {
            if (value.contains(probe)) {
                return true;
            }
        }
        return false;
    }
}
