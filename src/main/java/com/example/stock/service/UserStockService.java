package com.example.stock.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.example.stock.model.Tickers;
import com.example.stock.model.UserStock;
import com.example.stock.model.Users;
import com.example.stock.repository.UserStockRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserStockService {
	private final UserStockRepository userStockRepository;
	private final TickersService tickersService;

	public BigDecimal getStockQuantityByUserAndTicker(Users user, String symbol) {
		Tickers ticker = tickersService.getTickersBySymbol(symbol);
		BigDecimal quantity = BigDecimal.ZERO;
		UserStock userStock = userStockRepository.findByUserAndTicker(user, ticker);
		if (userStock != null && userStock.getQuantity() != null) {
			quantity = userStock.getQuantity();
		}

		return quantity;
	}
}