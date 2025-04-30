package com.example.stock.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.stock.model.Tickers;
import com.example.stock.model.Trade;
import com.example.stock.model.UserStock;
import com.example.stock.model.Users;
import com.example.stock.repository.UserStockRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ユーザーの株式保有数を更新するサービス（保有数チェック＆更新のみ）
 */
@Slf4j
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

	/**
	 * 最終的に保有株数を更新する（買いなら加算、売りなら減算）
	 * 売りのときは最終的に持株数チェックも行う
	 */
	@Transactional
	public void applyTradeToUserStock(Trade trade) {
		Users user = trade.getUser();
		Tickers ticker = trade.getTicker();
		BigDecimal tradeQty = trade.getQuantity();

		UserStock userStock = userStockRepository.findByUserAndTicker(user, ticker)
				.orElseGet(() -> {
					// 売り注文で保有がない場合はエラー
					if (trade.getSide() == 1) {
						throw new IllegalStateException("保有していない株式を売却することはできません");
					}
					UserStock newStock = new UserStock();
					newStock.setUser(user);
					newStock.setTicker(ticker);
					newStock.setQuantity(BigDecimal.ZERO);
					newStock.setCreateAt(LocalDateTime.now());
					return newStock;
				});

		// 【売り注文】保有株数チェック（最終検証）
		if (trade.getSide() == 1) {
			BigDecimal currentQty = userStock.getQuantity();
			if (currentQty.compareTo(tradeQty) < 0) {
				log.error("【保有株エラー】取引ID：{}, ユーザーID: {}, 銘柄: {}, 売却数: {}, 保有数: {}",
						trade.getId(), user.getId(), ticker.getTicker(), tradeQty, currentQty);
				throw new IllegalStateException("【最終検証】保有株数が不足しています");
			}
		}

		// 株数の更新
		if (trade.getSide() == 0) {
			userStock.setQuantity(userStock.getQuantity().add(tradeQty));
		} else if (trade.getSide() == 1) {
			userStock.setQuantity(userStock.getQuantity().subtract(tradeQty));
		}

		userStock.setUpdateAt(LocalDateTime.now());
		userStockRepository.save(userStock);

		log.info("【保有株更新】ユーザーID: {}, 銘柄: {}, 処理数量: {}, 残り: {}",
				user.getId(), ticker.getTicker(), tradeQty, userStock.getQuantity());
	}
}
