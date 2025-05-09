package com.example.stock.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.stock.model.OtpToken;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
	Optional<OtpToken> findTopByEmailOrderByExpiryTimeDesc(String email);

	void deleteByEmail(String email);
}
