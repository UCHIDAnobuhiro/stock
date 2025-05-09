package com.example.stock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;

import com.example.stock.model.OtpToken;
import com.example.stock.repository.OtpTokenRepository;

public class OtpServiceTest {

	@Mock
	private OtpTokenRepository otpTokenRepository;

	@Mock
	private JavaMailSender mailSender;

	@InjectMocks
	private OtpService otpService;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
	}

	// F-007-TC1 OTPの生成と送信が正常に動作すること
	@Test
	void testGenerateAndSendOtp() {
		String email = "test@example.com";
		otpService.generateAndSendOtp(email);
		verify(otpTokenRepository, times(1)).save(any());

	}

	// F-007-TC2 正しいOTPで認証が成功すること
	@Test
	void testVerifyOtp_Success() {
		String email = "test@example.com";
		String otp = "123456";
		OtpToken token = new OtpToken();
		token.setEmail(email);
		token.setOtp(otp);
		token.setExpiryTime(LocalDateTime.now().plusMinutes(5));

		when(otpTokenRepository.findTopByEmailOrderByExpiryTimeDesc(email)).thenReturn(Optional.of(token));

		boolean result = otpService.verifyOtp(email, otp);
		assertTrue(result);
	}

	// F-007-TC3 間違ったOTPで認証が失敗すること
	@Test
	void testVerifyOtp_InvalidOtp() {
		String email = "test@example.com";
		String otp = "123456";
		OtpToken token = new OtpToken();
		token.setEmail(email);
		token.setOtp("654321"); // 間違ったOTPを設定
		token.setExpiryTime(LocalDateTime.now().plusHours(1));

		when(otpTokenRepository.findTopByEmailOrderByExpiryTimeDesc(email)).thenReturn(Optional.of(token));

		boolean result = otpService.verifyOtp(email, otp);
		assertFalse(result);
	}

	// F-007-TC4 期限切れのOTPで認証が失敗すること
	@Test
	void testVerifyOtp_Expired() {
		String email = "test@example.com";
		String otp = "123456";
		OtpToken token = new OtpToken();
		token.setEmail(email);
		token.setOtp(otp);
		token.setExpiryTime(LocalDateTime.now().minusMinutes(1)); // 期限切れ

		when(otpTokenRepository.findTopByEmailOrderByExpiryTimeDesc(email)).thenReturn(Optional.of(token));

		boolean result = otpService.verifyOtp(email, otp);
		assertFalse(result);
	}

}
