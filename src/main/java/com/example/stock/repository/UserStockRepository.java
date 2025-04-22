package com.example.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.stock.model.Tickers;
import com.example.stock.model.UserStock;
import com.example.stock.model.Users;

@Repository
public interface UserStockRepository extends JpaRepository<UserStock, Long> {
	UserStock findByUserAndTicker(Users user, Tickers ticker);
}
