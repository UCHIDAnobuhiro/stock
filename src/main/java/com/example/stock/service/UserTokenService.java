package com.example.stock.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.stock.enums.TokenType;
import com.example.stock.model.UserToken;
import com.example.stock.model.Users;
import com.example.stock.repository.UserTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserTokenService {
	private final UserTokenRepository userTokenRepository;

	public UserToken createToken(Users user, TokenType type, Duration validDuration) {
		// 古いトークン削除（1ユーザーに1トークン制限）
		userTokenRepository.deleteByUserAndType(user, type);

		String token = UUID.randomUUID().toString();
		LocalDateTime expiryDate = LocalDateTime.now().plus(validDuration);

		UserToken userToken = new UserToken();
		userToken.setToken(token);
		userToken.setType(type);
		userToken.setUser(user);
		userToken.setExpiryDate(expiryDate);

		return userTokenRepository.save(userToken);
	}

	public Optional<UserToken> validateToken(String token, TokenType type) {
		return userTokenRepository.findByTokenAndType(token, type)
				.filter(t -> !t.isExpired());
	}

	public void deleteToken(Users user, TokenType type) {
		userTokenRepository.deleteByUserAndType(user, type);
	}

}
