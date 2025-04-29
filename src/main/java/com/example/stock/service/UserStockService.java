package com.example.stock.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.stock.model.Tickers;
import com.example.stock.model.Trade;
import com.example.stock.model.UserStock;
import com.example.stock.model.UserWallet;
import com.example.stock.model.Users;
import com.example.stock.repository.UserStockRepository;
import com.example.stock.util.TradeValidationUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ユーザーが保有する銘柄（株式）の数量を管理するサービスクラス。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStockService {

	private final UserStockRepository userStockRepository;
	private final TickersService tickersService;
	private final UserWalletService userWalletService;
	private final StockService stockService;

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
	 * 取引に基づいてユーザーの保有株数を更新。
	 * 買い注文: 加算、売り注文: 減算
	 * 不正のエラー時はログを出力
	 * @param trade 実行された取引
	 */
	public void applyTradeToUserStock(Trade trade) {
		Users user = trade.getUser();
		Tickers ticker = trade.getTicker();
		BigDecimal tradeQty = trade.getQuantity();
		UserWallet wallet = userWalletService.getWalletByUser(user);
		boolean isAvailable = true;

		// 買い（残高減少）前にチェックし、ログ処理する
		if (!TradeValidationUtil.isBalanceEnough(trade, wallet)) {
			log.error("【残高エラー】取引ID：{}, ユーザーID: {}, 通貨: {}, 必要金額: {}, 残高: {}",
					trade.getId(), user.getId(), trade.getSettlementCurrency(),
					trade.getTotalPrice(),
					"JPY".equalsIgnoreCase(trade.getSettlementCurrency())
							? wallet.getJpyBalance()
							: wallet.getUsdBalance());
			isAvailable = false;
		}

		//値幅制限チェック
		if (!TradeValidationUtil.isWithinLimit(trade, stockService)) {
			BigDecimal[] range = TradeValidationUtil.getPriceLimitRange(trade, stockService);
			log.error("【価格制限エラー】取引ID：{}, ユーザーID: {}, 単価: {}, 許容範囲: {} ～ {}",
					trade.getId(), user.getId(), trade.getUnitPrice(),
					range[0], range[1]);
			isAvailable = false;

		}

		//エラーが発生なら、更新しない
		if (!isAvailable) {
			return;
		}

		//保有があるかを確認、ない場合は新規か異常を出すか
		UserStock userStock = userStockRepository.findByUserAndTicker(user, ticker)
				.orElseGet(() -> {
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

		// 株数更新
		if (trade.getSide() == 0) {
			userStock.setQuantity(userStock.getQuantity().add(tradeQty));
		} else if (trade.getSide() == 1) {
			if (userStock.getQuantity().compareTo(tradeQty) < 0) {
				throw new IllegalArgumentException("売却数量が保有数量を超えています");
			}
			userStock.setQuantity(userStock.getQuantity().subtract(tradeQty));
		}

		userStock.setUpdateAt(LocalDateTime.now());
		userStockRepository.save(userStock);
	}
}