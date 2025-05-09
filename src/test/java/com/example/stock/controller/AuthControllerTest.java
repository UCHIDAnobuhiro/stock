package com.example.stock.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.stock.repository.OtpTokenRepository;
import com.example.stock.service.OtpService;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc
public class AuthControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private OtpService otpService;

	@MockBean
	private OtpTokenRepository otpTokenRepository;

	private final String TEST_EMAIL = "test@example.com";

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
		String otp = "123456";

		// OtpService.verifyOtp の戻り値をモックする
		when(otpService.verifyOtp(TEST_EMAIL, otp)).thenReturn(true);

		mockMvc.perform(post("/verify-otp")
				.param("otp", otp))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/logo/detect"));
	}

	// F-007-TC7 間違ったOTPで認証が失敗し、リダイレクトされること
	@Test
	@WithMockUser(username = "test@example.com")
	void testVerifyOtp_Fail() throws Exception {
		String otp = "000000";

		// OtpService.verifyOtp の戻り値をモックする
		when(otpService.verifyOtp(TEST_EMAIL, otp)).thenReturn(false);

		mockMvc.perform(post("/verify-otp")
				.param("otp", "000000"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/otp"));
	}
}
