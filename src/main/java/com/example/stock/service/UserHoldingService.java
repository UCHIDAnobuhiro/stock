package com.example.stock.service;

import org.springframework.stereotype.Service;

import com.example.stock.model.Tickers;
import com.example.stock.model.UserHolding;
import com.example.stock.model.Users;
import com.example.stock.repository.UserHoldingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserHoldingService {
	private final UserHoldingRepository userHoldingRepository;
	private final TickersService tickersService;

	public UserHolding getHoldingByUserAndTicker(Users user, String symbol) {
		Tickers ticker = tickersService.getTickersBySymbol(symbol);
		return userHoldingRepository.findByUserAndTicker(user, ticker);
	}
}
