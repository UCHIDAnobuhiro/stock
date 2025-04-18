package com.example.stock.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.example.stock.model.Tickers;
import com.example.stock.model.UserStock;
import com.example.stock.model.Users;
import com.example.stock.repository.UserStockRepository;

import lombok.RequiredArgsConstructor;

/**
 * ユーザーが保有する銘柄（株式）の数量を管理するサービスクラス。
 */
@Service
@RequiredArgsConstructor
public class UserStockService {

	private final UserStockRepository userStockRepository;
	private final TickersService tickersService;

	/**
	 * 指定されたユーザーが特定の銘柄をどれだけ保有しているか（株数）を取得する。
	 * 保有していない場合は 0 を返す。
	 *
	 * @param user ユーザー
	 * @param symbol 銘柄のシンボル名（例: "AAPL"）
	 * @return 該当ユーザーの保有株数（存在しない場合は BigDecimal.ZERO）
	 */
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
