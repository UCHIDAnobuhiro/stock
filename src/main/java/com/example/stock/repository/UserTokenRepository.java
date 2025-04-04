package com.example.stock.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.stock.enums.TokenType;
import com.example.stock.model.UserToken;
import com.example.stock.model.Users;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
	Optional<UserToken> findByTokenAndType(String token, TokenType type);

	void deleteByUserAndType(Users user, TokenType type); // 既存トークン削除に使う

}
