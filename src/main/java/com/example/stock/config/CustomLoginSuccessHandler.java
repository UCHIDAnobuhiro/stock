package com.example.stock.config;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.stock.repository.UsersRepository;
import com.example.stock.service.OtpService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final UsersRepository usersRepository;
	private final OtpService otpService;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication)
			throws IOException, ServletException {
		String email = authentication.getName();

		// 試行回数リセット（ロックも解除）
		usersRepository.findByEmail(email).ifPresent(user -> {
			if (user.getFailedLoginAttempts() > 0 || user.isAccountLocked()) {
				user.setFailedLoginAttempts(0);
				user.setAccountLocked(false);
				user.setLockTime(null);
				usersRepository.save(user);
			}
		});

		// 6桁のOTPを生成してメール送信
		otpService.generateAndSendOtp(email);

		// 毎回必ず /otp にリダイレクト
		setDefaultTargetUrl("/otp");

		super.onAuthenticationSuccess(request, response, authentication);

	}
}