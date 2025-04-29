package com.example.stock.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.stock.model.Tickers;
import com.example.stock.model.UserStock;
import com.example.stock.model.Users;

@Repository
public interface UserStockRepository extends JpaRepository<UserStock, Long> {
	Optional<UserStock> findByUserAndTicker(Users user, Tickers ticker);
}
