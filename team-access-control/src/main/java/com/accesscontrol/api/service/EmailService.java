package com.accesscontrol.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendInvitationEmail(String toEmail, String orgName, String rawToken) {
        String link = frontendUrl + "/accept-invite?token=" + rawToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("You've been invited to join " + orgName);
        message.setText(
            "You've been invited to join " + orgName + " on Team Access Control.\n\n" +
            "Click below to accept:\n" + link + "\n\n" +
            "This link expires in 7 days."
        );
        mailSender.send(message);
    }
}