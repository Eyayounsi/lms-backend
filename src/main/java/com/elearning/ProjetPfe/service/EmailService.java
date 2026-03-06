package com.elearning.ProjetPfe.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

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