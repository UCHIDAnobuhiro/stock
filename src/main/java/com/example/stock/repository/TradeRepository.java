package com.example.stock.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.stock.model.Trade;
import com.example.stock.model.Users;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

	List<Trade> findByUserOrderByCreateAtDesc(Users user);

	List<Trade> findByUserAndCreateAtAfterOrderByCreateAtDesc(Users user, LocalDateTime from);

}
