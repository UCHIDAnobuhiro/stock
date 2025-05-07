package com.example.stock.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.example.stock.model.Trade;
import com.example.stock.model.UserWallet;
import com.example.stock.service.StockService;
import com.example.stock.service.UserStockService;

public class TradeValidationUtil {

	/**
	 * 残高チェック（買い注文のみ対象）
	 * @param trade 対象取引
	 * @param wallet 該当ユーザーのウォレット
	 * @return boolean（true: OK, false: 残高不足）
	 * @throws IllegalArgumentException 所有者不一致
	 */
	public static boolean isBalanceEnough(Trade trade, UserWallet wallet) {
		if (!trade.getUser().getId().equals(wallet.getUser().getId())) {
			throw new IllegalArgumentException("TradeとWalletの所有者が一致しません");
		}

		if (trade.getSide() != 0) {
			return true; // 売り注文はチェック不要
		}

		BigDecimal balance = "JPY".equalsIgnoreCase(trade.getSettlementCurrency())
				? wallet.getJpyBalance()
				: wallet.getUsdBalance();

		return balance.compareTo(trade.getTotalPrice()) >= 0;
	}

	/**
	 * 値幅制限チェック（±10%）
	 * @param trade 対象取引
	 * @param stockService 現在値取得用のサービス
	 * @return boolean（true: OK, false: 超過）
	 */
	public static boolean isWithinLimit(Trade trade, StockService stockService) {
		BigDecimal[] range = getPriceLimitRange(trade, stockService);
		BigDecimal unitPrice = trade.getUnitPrice();

		return unitPrice.compareTo(range[0]) >= 0 && unitPrice.compareTo(range[1]) <= 0;
	}

	/**
	 * ※テスト用のメソッドです。本番コードでは {@link #isWithinLimit(Trade, StockService)} を使用してください。
	 */
	public static boolean isWithinLimit(Trade trade, BigDecimal lowerLimit, BigDecimal upperLimit) {
		BigDecimal unitPrice = trade.getUnitPrice();
		return unitPrice.compareTo(lowerLimit) >= 0 && unitPrice.compareTo(upperLimit) <= 0;
	}

	/**
	 * 現在値 ±10% の価格範囲を取得
	 * @param trade 対象取引
	 * @param stockService 現在値取得用
	 * @return [下限, 上限]
	 */
	public static BigDecimal[] getPriceLimitRange(Trade trade, StockService stockService) {
		String symbol = trade.getTicker().getTicker();
		BigDecimal closePrice = BigDecimal
				.valueOf(stockService.getLatestStockWithPrevClose(symbol).getClose())
				.setScale(2, RoundingMode.HALF_UP);

		BigDecimal lower = closePrice.multiply(BigDecimal.valueOf(0.9)).setScale(2, RoundingMode.HALF_UP);
		BigDecimal upper = closePrice.multiply(BigDecimal.valueOf(1.1)).setScale(2, RoundingMode.HALF_UP);

		return new BigDecimal[] { lower, upper };
	}

	/**
	 * 売り注文の場合、保有数以上の売却を防ぐチェック
	 * @param trade 売り注文
	 * @param wallet 該当ユーザーのウォレット
	 * @param userStockService ユーザーの保有株数を取得できるサービス
	 * @return boolean（true: OK, false: 保有数不足）
	 */
	public static boolean isSellQuantityEnough(Trade trade, UserStockService userStockService) {
		if (trade.getSide() != 1) {
			return true; // 買い注文はチェック不要
		}

		BigDecimal holdingQuantity = userStockService.getStockQuantityByUserAndTicker(trade.getUser(),
				trade.getTicker().getTicker());
		return holdingQuantity.compareTo(trade.getQuantity()) >= 0;
	}
}
