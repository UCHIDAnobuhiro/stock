package com.example.stock.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.stock.model.OtpToken;
import com.example.stock.repository.OtpTokenRepository;

@SpringBootTest
@AutoConfigureMockMvc
public class OtpServiceTest {
	@Autowired
	private OtpService otpService;

	@Autowired
	private OtpTokenRepository otpTokenRepository;

	@AfterEach
	void cleanUp() {
		otpTokenRepository.deleteAll();
	}

	// F-007-TC1 OTPの生成と送信が正常に動作すること
	@Test
	void testGenerateAndSendOtp() {
		String email = "test@example.com";
		otpService.generateAndSendOtp(email);

		List<OtpToken> tokens = otpTokenRepository.findAll();
		assertFalse(tokens.isEmpty());

		OtpToken otpToken = tokens.get(0);
		assertEquals(email, otpToken.getEmail());
		assertNotNull(otpToken.getOtp());
		assertTrue(otpToken.getExpiryTime().isAfter(LocalDateTime.now()));
	}

	// F-007-TC2 正しいOTPで認証が成功すること
	@Test
	void testVerifyOtp_Success() {
		String email = "test@example.com";
		otpService.generateAndSendOtp(email);

		OtpToken latestToken = otpTokenRepository.findTopByEmailOrderByExpiryTimeDesc(email).orElseThrow();
		String otp = latestToken.getOtp();

		boolean result = otpService.verifyOtp(email, otp);
		assertTrue(result);
	}

	// F-007-TC3 間違ったOTPで認証が失敗すること
	@Test
	void testVerifyOtp_InvalidOtp() {
		String email = "test@example.com";
		otpService.generateAndSendOtp(email);

		boolean result = otpService.verifyOtp(email, "000000"); // 間違ったOTP
		assertFalse(result);
	}

	// F-007-TC4 期限切れのOTPで認証が失敗すること
	@Test
	void testVerifyOtp_Expired() {
		String email = "test@example.com";
		String otp = "123456";

		OtpToken expiredToken = new OtpToken();
		expiredToken.setEmail(email);
		expiredToken.setOtp(otp);
		expiredToken.setExpiryTime(LocalDateTime.now().minusMinutes(1)); // 期限切れ
		otpTokenRepository.save(expiredToken);

		boolean result = otpService.verifyOtp(email, otp);
		assertFalse(result);
	}

}
