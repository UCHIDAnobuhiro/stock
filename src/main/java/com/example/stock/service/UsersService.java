package com.example.stock.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import jakarta.transaction.Transactional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.stock.exception.UserRegistrationException;
import com.example.stock.model.Users;
import com.example.stock.model.VerificationToken;
import com.example.stock.repository.UsersRepository;
import com.example.stock.repository.VerificationTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsersService {
	private final UsersRepository usersRepository;
	private final PasswordEncoder passwordEncoder;
	private final VerificationTokenRepository tokenRepository;
	private final MailService mailService;

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
			throw new UserRegistrationException("email", "メールアドレスを入力してください");
		}

		if (usersRepository.findByEmail(users.getEmail()).isPresent()) {
			throw new UserRegistrationException("email", "このメールアドレスは既に登録されています");
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
		users.setEnabled(false);

		// ユーザーをデータベースに保存
		usersRepository.save(users);

		String token = UUID.randomUUID().toString();
		VerificationToken verificationToken = new VerificationToken();
		verificationToken.setToken(token);
		verificationToken.setUser(users);
		verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24));
		tokenRepository.save(verificationToken);

		mailService.sendVerificationEmail(users.getEmail(), token);

	}

	@Transactional
	public boolean verifyUser(String token) {
		Optional<VerificationToken> optionalToken = tokenRepository.findByToken(token);
		if (optionalToken.isEmpty())
			return false;

		VerificationToken verificationToken = optionalToken.get();

		if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
			tokenRepository.delete(verificationToken);
			return false;
		}

		Users user = verificationToken.getUser();
		user.setEnabled(true);
		usersRepository.save(user);
		tokenRepository.delete(verificationToken);
		return true;
	}

}
