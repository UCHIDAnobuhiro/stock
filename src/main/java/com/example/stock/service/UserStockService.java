package com.example.stock.service;

import org.springframework.stereotype.Service;

import com.example.stock.model.Tickers;
import com.example.stock.model.UserStock;
import com.example.stock.model.Users;
import com.example.stock.repository.UserStockRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserStockService {
	private final UserStockRepository userHoldingRepository;
	private final TickersService tickersService;

	public UserStock getHoldingByUserAndTicker(Users user, String symbol) {
		Tickers ticker = tickersService.getTickersBySymbol(symbol);
		return userHoldingRepository.findByUserAndTicker(user, ticker);
	}
}
