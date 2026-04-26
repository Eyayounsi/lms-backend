package com.elearning.ProjetPfe.service.communication;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    // ═══════════════════════════════════════════════════════════════════════
    //  Payout : virement approuvé
    // ═══════════════════════════════════════════════════════════════════════

    public void sendPayoutPaid(String to, String name, BigDecimal amount,
                               String period, String paidAt, String notes) {
        String subject = "✅ Votre virement de " + amount + " € a été effectué";
        String html = "<html><body style='font-family:Arial,sans-serif;background:#f4f7fa;margin:0;padding:0;'>"
            + "<div style='max-width:600px;margin:40px auto;background:#fff;border-radius:12px;"
            + "box-shadow:0 2px 12px rgba(0,0,0,0.08);overflow:hidden;'>"
            + "<div style='background:#22c55e;padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#fff;margin:0;font-size:26px;'>✅ Virement effectué</h1></div>"
            + "<div style='padding:36px;'>"
            + "<p style='font-size:16px;color:#374151;'>Bonjour <strong>" + escHtml(name) + "</strong>,</p>"
            + "<p style='color:#6b7280;'>Votre demande de virement a été traitée avec succès.</p>"
            + "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-radius:8px;padding:20px;margin:24px 0;'>"
            + "<table style='width:100%;border-collapse:collapse;'>"
            + "<tr><td style='padding:6px 0;color:#6b7280;'>Montant versé</td>"
            + "<td style='padding:6px 0;text-align:right;font-weight:700;color:#16a34a;font-size:20px;'>"
            + amount + " €</td></tr>"
            + "<tr><td style='padding:6px 0;color:#6b7280;'>Période</td>"
            + "<td style='padding:6px 0;text-align:right;font-weight:600;'>" + escHtml(period) + "</td></tr>"
            + "<tr><td style='padding:6px 0;color:#6b7280;'>Date de paiement</td>"
            + "<td style='padding:6px 0;text-align:right;'>" + escHtml(paidAt) + "</td></tr>"
            + (notes != null && !notes.isBlank()
                ? "<tr><td style='padding:6px 0;color:#6b7280;'>Note admin</td>"
                + "<td style='padding:6px 0;text-align:right;font-style:italic;'>" + escHtml(notes) + "</td></tr>"
                : "")
            + "</table></div>"
            + "<p style='color:#9ca3af;font-size:12px;margin-top:28px;'>"
            + "Cet email est envoyé automatiquement. Ne pas répondre.</p>"
            + "</div></div></body></html>";
        sendHtml(to, subject, html);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Payout : virement rejeté
    // ═══════════════════════════════════════════════════════════════════════

    public void sendPayoutRejected(String to, String name, BigDecimal amount,
                                   String period, String reason) {
        String subject = "❌ Votre demande de virement a été refusée";
        String html = "<html><body style='font-family:Arial,sans-serif;background:#f4f7fa;margin:0;padding:0;'>"
            + "<div style='max-width:600px;margin:40px auto;background:#fff;border-radius:12px;"
            + "box-shadow:0 2px 12px rgba(0,0,0,0.08);overflow:hidden;'>"
            + "<div style='background:#ef4444;padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#fff;margin:0;font-size:26px;'>❌ Virement refusé</h1></div>"
            + "<div style='padding:36px;'>"
            + "<p style='font-size:16px;color:#374151;'>Bonjour <strong>" + escHtml(name) + "</strong>,</p>"
            + "<p style='color:#6b7280;'>Votre demande de virement de <strong>" + amount
            + " €</strong> (période : " + escHtml(period) + ") a été refusée.</p>"
            + "<div style='background:#fef2f2;border:1px solid #fecaca;border-radius:8px;padding:16px;margin:20px 0;'>"
            + "<p style='margin:0;color:#991b1b;'><strong>Raison :</strong> "
            + (reason != null && !reason.isBlank() ? escHtml(reason) : "Non précisée") + "</p></div>"
            + "<p style='color:#6b7280;'>Votre solde a été restauré. "
            + "Vous pouvez soumettre une nouvelle demande depuis votre tableau de bord.</p>"
            + "<p style='color:#9ca3af;font-size:12px;margin-top:28px;'>"
            + "Cet email est envoyé automatiquement. Ne pas répondre.</p>"
            + "</div></div></body></html>";
        sendHtml(to, subject, html);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Envoi HTML générique (ne brise pas le flux si l'envoi échoue)
    // ═══════════════════════════════════════════════════════════════════════

    private void sendHtml(String to, String subject, String html) {
        if (!isMailConfigured("sendHtml", to)) {
            return;
        }

        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("[EMAIL] Échec envoi à {} : {}", to, e.getMessage());
        }
    }

    /** Échappe les caractères HTML pour éviter l'injection dans les templates email. */
    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#x27;");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Méthodes existantes
    // ═══════════════════════════════════════════════════════════════════════

    public void sendOtpEmail(String toEmail, String otpCode) {
        if (!isMailConfigured("sendOtpEmail", toEmail)) return;

        String subject = "🔑 Réinitialisation de votre mot de passe — LMS";
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
            + "<body style='margin:0;padding:0;font-family:\"Segoe UI\",Arial,sans-serif;background:#f0f2ff;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f0f2ff;padding:40px 20px;'>"
            + "<tr><td align='center'>"
            + "<table width='580' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:16px;overflow:hidden;"
            + "box-shadow:0 4px 24px rgba(92,103,229,0.12);'>"
            // Header
            + "<tr><td style='background:linear-gradient(135deg,#7C3AED 0%,#9B59F5 100%);padding:40px 48px;text-align:center;'>"
            + "<h1 style='color:#fff;margin:0;font-size:26px;font-weight:700;letter-spacing:-0.5px;'>🔑 Réinitialisation du mot de passe</h1>"
            + "<p style='color:rgba(255,255,255,0.85);margin:10px 0 0;font-size:15px;'>LMS — Plateforme d'apprentissage</p>"
            + "</td></tr>"
            // Body
            + "<tr><td style='padding:40px 48px;'>"
            + "<p style='font-size:15px;color:#6b7280;margin:0 0 24px;line-height:1.6;'>"
            + "Vous avez demandé la réinitialisation de votre mot de passe. Entrez le code ci-dessous dans le formulaire :</p>"
            // OTP box
            + "<div style='background:#faf5ff;border:2px solid #7C3AED;border-radius:12px;padding:32px;text-align:center;margin:0 0 28px;'>"
            + "<p style='margin:0 0 8px;font-size:12px;color:#7C3AED;text-transform:uppercase;letter-spacing:1.5px;font-weight:700;'>Code de vérification</p>"
            + "<p style='margin:0;font-size:52px;font-weight:800;color:#7C3AED;letter-spacing:18px;font-family:\"Courier New\",monospace;'>"
            + escHtml(otpCode) + "</p>"
            + "<p style='margin:12px 0 0;font-size:13px;color:#9ca3af;'>⏱ Valable <strong>10 minutes</strong></p>"
            + "</div>"
            + "<div style='background:#fef9c3;border-left:4px solid #eab308;padding:14px 16px;border-radius:6px;margin:0 0 28px;'>"
            + "<p style='margin:0;font-size:14px;color:#92400e;'>⚠️ Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.</p></div>"
            + "<p style='font-size:13px;color:#9ca3af;margin:0;'>Cet email est envoyé automatiquement. Ne pas répondre.</p>"
            + "</td></tr>"
            // Footer
            + "<tr><td style='background:#f9fafb;padding:20px 48px;text-align:center;border-top:1px solid #e5e7eb;'>"
            + "<p style='margin:0;font-size:12px;color:#9ca3af;'>© 2025 LMS Platform — Tous droits réservés</p>"
            + "</td></tr>"
            + "</table></td></tr></table></body></html>";

        sendHtml(toEmail, subject, html);
    }

    public void sendRegisterOtpEmail(String toEmail, String fullName, String otpCode) {
        if (!isMailConfigured("sendRegisterOtpEmail", toEmail)) return;

        String subject = "✉️ Vérifiez votre email pour finaliser votre inscription — LMS";
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
            + "<body style='margin:0;padding:0;font-family:\"Segoe UI\",Arial,sans-serif;background:#f0f2ff;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f0f2ff;padding:40px 20px;'>"
            + "<tr><td align='center'>"
            + "<table width='580' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:16px;overflow:hidden;"
            + "box-shadow:0 4px 24px rgba(92,103,229,0.12);'>"
            // Header
            + "<tr><td style='background:linear-gradient(135deg,#5C67E5 0%,#7B87FF 100%);padding:40px 48px;text-align:center;'>"
            + "<h1 style='color:#fff;margin:0;font-size:28px;font-weight:700;letter-spacing:-0.5px;'>🎓 Bienvenue sur LMS !</h1>"
            + "<p style='color:rgba(255,255,255,0.85);margin:10px 0 0;font-size:15px;'>Vérification de votre adresse email</p>"
            + "</td></tr>"
            // Body
            + "<tr><td style='padding:40px 48px;'>"
            + "<p style='font-size:16px;color:#374151;margin:0 0 16px;'>Bonjour <strong>" + escHtml(fullName) + "</strong>,</p>"
            + "<p style='font-size:15px;color:#6b7280;margin:0 0 32px;line-height:1.6;'>"
            + "Merci de vous inscrire sur notre plateforme. Pour finaliser votre inscription, entrez le code ci-dessous :</p>"
            // OTP box
            + "<div style='background:#f0f2ff;border:2px solid #5C67E5;border-radius:12px;padding:32px;text-align:center;margin:0 0 32px;'>"
            + "<p style='margin:0 0 8px;font-size:12px;color:#5C67E5;text-transform:uppercase;letter-spacing:1.5px;font-weight:700;'>Votre code de vérification</p>"
            + "<p style='margin:0;font-size:52px;font-weight:800;color:#5C67E5;letter-spacing:18px;font-family:\"Courier New\",monospace;'>"
            + escHtml(otpCode) + "</p>"
            + "<p style='margin:12px 0 0;font-size:13px;color:#9ca3af;'>⏱ Ce code expire dans <strong>10 minutes</strong></p>"
            + "</div>"
            // Steps hint
            + "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-radius:8px;padding:16px 20px;margin:0 0 28px;'>"
            + "<p style='margin:0 0 8px;font-size:14px;font-weight:600;color:#166534;'>✅ Prochaine étape :</p>"
            + "<p style='margin:0;font-size:14px;color:#166534;'>Retournez sur la page d'inscription et entrez ce code pour créer votre compte.</p>"
            + "</div>"
            + "<div style='background:#fef9c3;border-left:4px solid #eab308;padding:14px 16px;border-radius:6px;margin:0 0 28px;'>"
            + "<p style='margin:0;font-size:14px;color:#92400e;'>⚠️ Si vous n'avez pas demandé cette inscription, ignorez cet email.</p></div>"
            + "<p style='font-size:13px;color:#9ca3af;margin:0;'>Cet email est envoyé automatiquement. Ne pas répondre.</p>"
            + "</td></tr>"
            // Footer
            + "<tr><td style='background:#f9fafb;padding:20px 48px;text-align:center;border-top:1px solid #e5e7eb;'>"
            + "<p style='margin:0;font-size:12px;color:#9ca3af;'>© 2025 LMS Platform — Tous droits réservés</p>"
            + "</td></tr>"
            + "</table></td></tr></table></body></html>";

        sendHtml(toEmail, subject, html);
    }

    /**
     * Méthode générique pour envoyer un email avec sujet et corps personnalisés.
     * Utilisée pour notifier l'instructor quand un cours est accepté/rejeté.
     */
    public void sendEmail(String toEmail, String subject, String body) {
        if (!isMailConfigured("sendEmail", toEmail)) return;
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("[EMAIL] Échec envoi texte à {} : {}", toEmail, e.getMessage());
        }
    }

    private boolean isMailConfigured(String operation, String toEmail) {
        if (mailSender == null) {
            log.error("[EMAIL] {} impossible pour {} : JavaMailSender indisponible", operation, toEmail);
            return false;
        }

        if (from == null || from.isBlank()) {
            log.error("[EMAIL] {} impossible pour {} : spring.mail.username vide", operation, toEmail);
            return false;
        }

        return true;
    }
}