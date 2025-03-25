package com.example.stock.service;

import java.time.LocalDateTime;

import jakarta.transaction.Transactional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.stock.exception.UserRegistrationException;
import com.example.stock.model.Users;
import com.example.stock.repository.UsersRepository;

@Service
public class UsersService {
	private final UsersRepository userssRepository;
	private final PasswordEncoder passwordEncoder;

	public UsersService(UsersRepository userssRepository, PasswordEncoder passwordEncoder) {
		this.userssRepository = userssRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public Users getLoggedInUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof Users) {
			return (Users) authentication.getPrincipal();
		}
		return null;
	}

	public Users getLoggedInUserOrThrow() {
		Users users = getLoggedInUser();
		if (users == null) {
			throw new IllegalStateException("ログインユーザが取得できません");
		}
		return users;
	}

	@Transactional
	public void registerUser(Users users) {
		if (users.getUsername() == null || users.getUsername().isBlank()) {
			throw new UserRegistrationException("name", "名前を入力してください");
		}

		if (users.getEmail() == null || users.getEmail().isBlank()) {
			throw new UserRegistrationException("mail", "メールアドレスを入力してください");
		}

		if (userssRepository.findByEmail(users.getEmail()).isPresent()) {
			throw new UserRegistrationException("mail", "このメールアドレスは既に登録されています");
		}

		if (users.getPassword() == null || users.getConfirmPassword() == null) {
			throw new UserRegistrationException("password", "パスワードを入力してください");
		}

		if (!users.getPassword().equals(users.getConfirmPassword())) {
			throw new UserRegistrationException("confirmPassword", "パスワードが一致しません");
		}

		// ユーザー情報をセット
		users.setCreateAt(LocalDateTime.now());
		users.setUpdateAt(LocalDateTime.now());
		users.setPassword(passwordEncoder.encode(users.getPassword()));

		// ユーザーをデータベースに保存
		userssRepository.save(users);

	}

}
