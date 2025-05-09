package com.example.stock.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.stock.repository.OtpTokenRepository;
import com.example.stock.service.OtpService;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc
public class AuthControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OtpService otpService;

	@Autowired
	private OtpTokenRepository otpTokenRepository;

	private final String TEST_EMAIL = "test@example.com";

	@BeforeEach
	void setup() {
		// ここでテスト用ユーザーを作るなど（必要なら）
	}

	@AfterEach
	void tearDown() {
		otpTokenRepository.deleteAll();
	}

	// F-007-TC5 OTP入力ページが正常に表示されること
	@Test
	@WithMockUser(username = "test@example.com")
	void testShowOtpPage() throws Exception {
		mockMvc.perform(get("/otp"))
				.andExpect(status().isOk())
				.andExpect(view().name("otp"));
	}

	// F-007-TC6 正しいOTPで認証が成功し、リダイレクトされること
	@Test
	@WithMockUser(username = "test@example.com")
	void testVerifyOtp_Success() throws Exception {
		otpService.generateAndSendOtp(TEST_EMAIL);
		String otp = otpTokenRepository.findTopByEmailOrderByExpiryTimeDesc(TEST_EMAIL).orElseThrow().getOtp();

		mockMvc.perform(post("/verify-otp")
				.param("otp", otp))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/logo/detect"));
	}

	// F-007-TC7 間違ったOTPで認証が失敗し、リダイレクトされること
	@Test
	@WithMockUser(username = "test@example.com")
	void testVerifyOtp_Fail() throws Exception {
		mockMvc.perform(post("/verify-otp")
				.param("otp", "000000"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/otp"));
	}
}
