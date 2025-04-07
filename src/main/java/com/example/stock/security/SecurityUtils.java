package com.example.stock.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.stock.model.Users;

@Component
public class SecurityUtils {
	public Users getLoggedInUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof Users) {
			return (Users) authentication.getPrincipal();
		}
		return null;
	}

	public Users getLoggedInUserOrThrow() {
		Users user = getLoggedInUser();
		if (user == null) {
			throw new IllegalStateException("ログインユーザが取得できません");
		}
		return user;
	}

}
