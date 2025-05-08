package com.example.stock.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.example.stock.model.OtpToken;
import com.example.stock.repository.OtpTokenRepository;
import com.example.stock.util.OtpUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtpService {
	private final OtpTokenRepository otpTokenRepository;
	private final JavaMailSender mailSender;

	public void generateAndSendOtp(String email) {
		String otp = OtpUtil.generateOtp();
		LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5);

		// Save OTP to DB
		OtpToken otpToken = new OtpToken();
		otpToken.setEmail(email);
		otpToken.setOtp(otp);
		otpToken.setExpiryTime(expiryTime);
		otpTokenRepository.save(otpToken);

		// Send OTP via email
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(email);
		message.setSubject("ワンタイムパスワード");
		message.setText("認証コードは: " + otp + "です。有効期限は5分です");

		mailSender.send(message);

	}

	public boolean verifyOtp(String email, String otpInput) {
		Optional<OtpToken> otpTokenOpt = otpTokenRepository.findTopByEmailOrderByExpiryTimeDesc(email);
		if (otpTokenOpt.isPresent()) {
			OtpToken otpToken = otpTokenOpt.get();
			if (otpToken.getOtp().equals(otpInput) && otpToken.getExpiryTime().isAfter(LocalDateTime.now())) {
				return true;
			}
		}
		return false;
	}

}
