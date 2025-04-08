package com.example.stock.config;

import java.io.IOException;
import java.time.LocalDateTime;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.example.stock.repository.UsersRepository;

@Component
public class CustomLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

	private static final int MAX_FAILED_ATTEMPTS = 5;
	private final UsersRepository usersRepository;

	public CustomLoginFailureHandler(UsersRepository usersRepository) {
		super("/login?error");
		this.usersRepository = usersRepository;
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authentication) throws IOException, ServletException {

		String email = request.getParameter("email");

		usersRepository.findByEmail(email).ifPresent(user -> {
			int attempts = user.getFailedLoginAttempts() + 1;
			user.setFailedLoginAttempts(attempts);
			if (attempts >= MAX_FAILED_ATTEMPTS) {
				user.setAccountLocked(true);
				user.setLockTime(LocalDateTime.now());
			}
			usersRepository.save(user);
		});

		super.onAuthenticationFailure(request, response, authentication);
	}
}
