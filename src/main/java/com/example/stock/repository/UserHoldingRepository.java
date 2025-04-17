package com.example.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.stock.model.Tickers;
import com.example.stock.model.UserHolding;
import com.example.stock.model.Users;

@Repository
public interface UserHoldingRepository extends JpaRepository<UserHolding, Long> {
	UserHolding findByUserAndTicker(Users user, Tickers ticker);
}
