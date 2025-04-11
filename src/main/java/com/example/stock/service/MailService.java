package com.example.stock.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.example.stock.enums.TokenType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailService {
	private static final Logger logger = LoggerFactory.getLogger(MailService.class);

	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;

	// 件名を定数化
	private static final String SUBJECT_VERIFICATION = "アカウント認証のご案内";
	private static final String SUBJECT_RESET_PASSWORD = "パスワード再設定のご案内";

	/**
	 * 認証メールを送信する（HTML+プレーンテキストのマルチパート対応）
	 *
	 * @param toEmail 送信先メールアドレス
	 * @param token 認証トークン
	 */
	public void sendVerificationEmail(String toEmail, String token, TokenType tokenType) {
		// リンクの初期化
		String link;

		// Thymleafのコンテキストに変数をセット
		Context context = new Context();

		// テンプレートを処理してHTML文字列を初期化
		String htmlBody;

		// プレーンテキストの本文を初期化
		String textBody;

		//現在のurlを取得
		String baseURL = getBaseUrlFromRequest();

		// プレーンテキスト版（シンプルにそのままリンク）
		if (tokenType == TokenType.VERIFY_EMAIL) {
			link = baseURL + "/verify/activate?token=" + token;
			context.setVariable("verificationLink", link);
			htmlBody = templateEngine.process("mail/verification-user", context);
			textBody = "以下のリンクをブラウザで開いて、認証を完了してください:\n" + link;
			logger.info("認証メールを送信します: to={}, link={}", toEmail, link);
		} else if (tokenType == TokenType.RESET_PASSWORD) {
			link = baseURL + "/password/reset?token=" + token;
			context.setVariable("resetPasswordLink", link);
			htmlBody = templateEngine.process("mail/verification-password", context);
			textBody = "以下のリンクをブラウザで開いて、パスワードの再設定を完了してください:\n" + link;
			logger.info("パスワード再設定メールを送信します: to={}, link={}", toEmail, link);
		} else {
			throw new IllegalArgumentException("不正なTokenTypeです: " + tokenType);
		}

		try {
			if (tokenType == TokenType.VERIFY_EMAIL) {
				sendHtmlEmail(toEmail, SUBJECT_VERIFICATION, htmlBody, textBody);
			} else if (tokenType == TokenType.RESET_PASSWORD) {
				sendHtmlEmail(toEmail, SUBJECT_RESET_PASSWORD, htmlBody, textBody);
			}
			logger.info("メール送信成功: to={}", toEmail);
		} catch (MessagingException e) {
			logger.error("メール送信失敗: to={}, エラー={}", toEmail, e.getMessage(), e);
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

	/**
	 * 現在のurlを取得
	 *現在のurlを取得http://localhost:8080/loginならhttp://localhost:8080/がとれる
	 */
	private String getBaseUrlFromRequest() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs != null) {
			HttpServletRequest request = attrs.getRequest();
			String baseUrl = request.getScheme() + "://" + request.getServerName()
					+ ((request.getServerPort() == 80 || request.getServerPort() == 443) ? ""
							: ":" + request.getServerPort());

			return baseUrl;
		}
		return null;
	}
}
