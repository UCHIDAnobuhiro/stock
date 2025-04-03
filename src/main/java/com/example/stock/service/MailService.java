package com.example.stock.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailService {
	private static final Logger logger = LoggerFactory.getLogger(MailService.class);

	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;

	// 件名を定数化
	private static final String SUBJECT_VERIFICATION = "アカウント認証のご案内";

	/**
	 * 認証メールを送信する（HTML+プレーンテキストのマルチパート対応）
	 *
	 * @param toEmail 送信先メールアドレス
	 * @param token 認証トークン
	 */
	public void sendVerificationEmail(String toEmail, String token) {
		String link = "http://localhost:8080/verify?token=" + token;
		logger.info("認証メールを送信します: to={}, link={}", toEmail, link);

		// Thymleafのコンテキストに変数をセット
		Context context = new Context();
		context.setVariable("verificationLink", link);

		// テンプレートを処理してHTML文字列を生成
		String htmlBody = templateEngine.process("mail/verification", context);

		// プレーンテキスト版（シンプルにそのままリンク）
		String textBody = "以下のリンクをブラウザで開いて、認証を完了してください:\n" + link;

		try {
			sendHtmlEmail(toEmail, SUBJECT_VERIFICATION, htmlBody, textBody);
			logger.info("認証メール送信成功: to={}", toEmail);
		} catch (MessagingException e) {
			logger.error("認証メール送信失敗: to={}, エラー={}", toEmail, e.getMessage(), e);
			throw new RuntimeException("メール送信に失敗しました", e);
		}

	}

	/**
	 * HTML + テキスト形式でメールを送信する
	 *
	 * @param to 宛先メールアドレス
	 * @param subject 件名
	 * @param htmlBody HTML本文
	 * @param textBody プレーンテキスト本文
	 */
	private void sendHtmlEmail(String to, String subject, String htmlBody, String textBody)
			throws MessagingException {
		// メールメッセージを作成
		MimeMessage message = mailSender.createMimeMessage();
		// マルチパートメールとしてヘルパーを初期化（true = multipart）
		MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

		helper.setTo(to); // 宛先を設定
		helper.setSubject(subject); // 件名を設定
		helper.setText(textBody, htmlBody);// テキスト版とHTML版の両方を設定

		mailSender.send(message);
	}

}
