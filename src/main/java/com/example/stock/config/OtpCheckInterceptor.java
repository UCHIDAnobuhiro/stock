package com.example.stock.config;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class OtpCheckInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws IOException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
			HttpSession session = request.getSession(false);
			Boolean otpVerified = (session != null) ? (Boolean) session.getAttribute("otpVerified") : null;

			String uri = request.getRequestURI();
			if ((otpVerified == null || !otpVerified)
					&& !uri.startsWith("/otp") && !uri.startsWith("/verify-otp") && !uri.startsWith("/logout")) {

				response.sendRedirect("/otp");
				return false;
			}
		}
		return true;
	}

}
