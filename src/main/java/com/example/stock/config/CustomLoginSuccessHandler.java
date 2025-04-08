package com.example.stock.config;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.stock.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final UsersRepository usersRepository;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication)
			throws IOException, ServletException {
		String email = authentication.getName(); // ログインしたユーザーのemail（username）

		// 試行回数リセット（ロックも解除）
		usersRepository.findByEmail(email).ifPresent(user -> {
			if (user.getFailedLoginAttempts() > 0 || user.isAccountLocked()) {
				user.setFailedLoginAttempts(0);
				user.setAccountLocked(false);
				user.setLockTime(null);
				usersRepository.save(user);
			}
		});

		response.sendRedirect("/stock");
	}
}