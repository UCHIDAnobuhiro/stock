package com.example.stock.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.stock.model.Tickers;
import com.example.stock.model.Trade;
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

		return userStockRepository.findByUserAndTicker(user, ticker)
				.map(UserStock::getQuantity)
				.orElse(BigDecimal.ZERO);
	}

	public void applyTradeToUserStock(Trade trade) {
		Users user = trade.getUser();
		Tickers ticker = trade.getTicker();
		BigDecimal tradeQty = trade.getQuantity();

		// 查询该用户是否已有该股票持仓
		UserStock userStock = userStockRepository.findByUserAndTicker(user, ticker)
				.orElseGet(() -> {
					// 如果没有，且是买入操作时新建（卖出则不合法）
					if (trade.getSide() == 1) {
						throw new IllegalArgumentException("保有していない株式を売却することはできません");
					}
					UserStock newStock = new UserStock();
					newStock.setUser(user);
					newStock.setTicker(ticker);
					newStock.setQuantity(BigDecimal.ZERO);
					newStock.setCreateAt(LocalDateTime.now());
					return newStock;
				});

		// 买入（加数量）
		if (trade.getSide() == 0) {
			userStock.setQuantity(userStock.getQuantity().add(tradeQty));
		}
		// 卖出（减数量）
		else if (trade.getSide() == 1) {
			if (userStock.getQuantity().compareTo(tradeQty) < 0) {
				throw new IllegalArgumentException("売却数量が保有数量を超えています");
			}
			userStock.setQuantity(userStock.getQuantity().subtract(tradeQty));
		}

		userStock.setUpdateAt(LocalDateTime.now());
		userStockRepository.save(userStock);
	}
}