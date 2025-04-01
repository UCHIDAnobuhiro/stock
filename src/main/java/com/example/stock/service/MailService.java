package com.example.stock.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailService {
	private final JavaMailSender mailSender;

	public void sendVerificationEmail(String toEmail, String token) {
		String subject = "アカウント認証のご案内";
		String link = "http://localhost:8080/verify?token=" + token;
		String content = "以下のリンクをクリックして認証を完了してください:\n" + link;

		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(toEmail);
		message.setSubject(subject);
		message.setText(content);

		mailSender.send(message);

	}

}
