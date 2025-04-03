package com.example.stock.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.stock.model.Users;
import com.example.stock.model.VerificationToken;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
	Optional<VerificationToken> findByToken(String token);

	void deleteAllByUser(Users user);

	@Modifying
	@Query("DELETE FROM VerificationToken vt WHERE vt.user = :user")
	void deleteByUser(@Param("user") Users user);
}