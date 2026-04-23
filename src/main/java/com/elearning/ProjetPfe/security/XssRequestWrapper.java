package com.elearning.ProjetPfe.security;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * Wrapper qui désinfecte les paramètres de requête et le corps JSON
 * contre les attaques XSS (Cross-Site Scripting).
 *
 * Stratégie :
 *  - Paramètres URL / form : strip de tout HTML dans chaque valeur
 *  - Corps JSON (application/json) : parse Jackson → sanitise chaque valeur
 *    texte individuellement → re-sérialise en JSON valide.
 *    (Passer le JSON brut dans OWASP encoderait " → &quot; et casserait le JSON)
 */
public class XssRequestWrapper extends HttpServletRequestWrapper {

    // Politique OWASP : aucun HTML autorisé dans les entrées utilisateur
    private static final PolicyFactory POLICY = new HtmlPolicyBuilder().toFactory();

    // Jackson : réutilisé (thread-safe après configuration)
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private byte[] sanitizedBody;

    public XssRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);

        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            InputStream is = request.getInputStream();
            String rawBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String cleanBody = sanitizeJsonBody(rawBody);
            this.sanitizedBody = cleanBody.getBytes(StandardCharsets.UTF_8);
        }
    }

    // ── URL/form parameters ──────────────────────────────────────────────

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return value != null ? sanitizeValue(value) : null;
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) return null;
        String[] cleaned = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleaned[i] = values[i] != null ? sanitizeValue(values[i]) : null;
        }
        return cleaned;
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        // Ne pas sanitiser les headers techniques
        if ("Authorization".equalsIgnoreCase(name) || "Content-Type".equalsIgnoreCase(name)) {
            return value;
        }
        return value != null ? sanitizeValue(value) : null;
    }

    // ── InputStream / Reader ─────────────────────────────────────────────

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (sanitizedBody == null) return super.getInputStream();
        ByteArrayInputStream bis = new ByteArrayInputStream(sanitizedBody);
        return new ServletInputStream() {
            @Override public int read() { return bis.read(); }
            @Override public boolean isFinished() { return bis.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener listener) {}
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (sanitizedBody == null) return super.getReader();
        return new BufferedReader(new InputStreamReader(
            new ByteArrayInputStream(sanitizedBody), StandardCharsets.UTF_8));
    }

    // ── JSON-aware sanitisation ──────────────────────────────────────────

    /**
     * Parse le corps JSON avec Jackson, sanitise chaque valeur texte
     * individuellement via OWASP, puis re-sérialise. Cela évite que OWASP
     * HTML-encode les délimiteurs JSON (guillemets → &quot;, etc.).
     * Si le corps n'est pas du JSON valide, il est retourné tel quel.
     */
    private static String sanitizeJsonBody(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) return rawBody;
        try {
            JsonNode root = MAPPER.readTree(rawBody);
            JsonNode cleaned = sanitizeNode(root);
            return MAPPER.writeValueAsString(cleaned);
        } catch (Exception e) {
            // Corps non-JSON (improbable ici) : ne pas modifier
            return rawBody;
        }
    }

    /** Parcours récursif : sanitise les nœuds texte, laisse les autres intacts. */
    private static JsonNode sanitizeNode(JsonNode node) {
        if (node.isTextual()) {
            String clean = sanitizeValue(node.asText());
            return MAPPER.valueToTree(clean);
        }
        if (node.isObject()) {
            ObjectNode obj = MAPPER.createObjectNode();
            node.fields().forEachRemaining(e -> obj.set(e.getKey(), sanitizeNode(e.getValue())));
            return obj;
        }
        if (node.isArray()) {
            ArrayNode arr = MAPPER.createArrayNode();
            node.forEach(item -> arr.add(sanitizeNode(item)));
            return arr;
        }
        // numbers, booleans, null → inchangés
        return node;
    }

    /**
     * Applique la politique OWASP sur une valeur texte individuelle,
     * puis redécode les entités HTML non dangereuses.
     *
     * Pourquoi : OWASP encode certains caractères en entités HTML (ex: @ → &#64;)
     * car il est conçu pour l'affichage HTML. Dans un contexte JSON/REST API,
     * on a besoin du texte brut. On redécode donc les entités sûres après
     * que OWASP ait strippé tous les tags dangereux.
     * Note : on NE redécode PAS &lt; et &gt; pour bloquer toute injection HTML résiduelle.
     */
    private static String sanitizeValue(String input) {
        if (input == null) return null;
        String sanitized = POLICY.sanitize(input);
        return decodeApiSafeEntities(sanitized);
    }

    /**
     * Redécode les entités HTML inoffensives pour restaurer le texte brut API.
     * L'ordre est important : &amp; doit être traité EN DERNIER pour éviter
     * le double-décodage (ex: &amp;amp; → & au lieu de &amp;).
     */
    private static String decodeApiSafeEntities(String s) {
        if (s == null || !s.contains("&")) return s;
        return s
            .replace("&#64;",  "@")    // signe @  (emails)
            .replace("&#43;",  "+")    // signe +
            .replace("&#46;",  ".")    // point (.)
            .replace("&#45;",  "-")    // tiret (-)
            .replace("&#95;",  "_")    // underscore
            .replace("&#47;",  "/")    // slash
            .replace("&#61;",  "=")    // signe =
            .replace("&#63;",  "?")    // point d'interrogation
            .replace("&#39;",  "'")    // apostrophe
            .replace("&quot;", "\"")   // guillemet double
            .replace("&nbsp;", " ")    // espace insécable
            .replace("&amp;",  "&");   // & (DOIT ÊTRE EN DERNIER)
        // Note délibérée : &lt; → < et &gt; → > ne sont PAS décodés
        // pour maintenir la protection contre les injections HTML < >
    }
}

