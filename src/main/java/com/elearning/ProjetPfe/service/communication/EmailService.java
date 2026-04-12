package com.elearning.ProjetPfe.service.communication;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
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
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject("Réinitialisation de votre mot de passe");
        message.setText("Bonjour,\n\n" +
                "Vous avez demandé la réinitialisation de votre mot de passe.\n\n" +
                "Voici votre code de vérification OTP : " + otpCode + "\n\n" +
                "Ce code est valable pendant 10 minutes.\n\n" +
                "Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.\n\n" +
                "Cordialement,\n" +
                "L'équipe E-Learning");

        mailSender.send(message);
    }

    /**
     * Méthode générique pour envoyer un email avec sujet et corps personnalisés.
     * Utilisée pour notifier l'instructor quand un cours est accepté/rejeté.
     */
    public void sendEmail(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}