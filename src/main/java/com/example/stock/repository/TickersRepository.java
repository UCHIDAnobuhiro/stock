package com.example.stock.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.stock.model.Tickers;

public interface TickersRepository extends JpaRepository<Tickers, Long> {
	List<Tickers> findAll();
}
