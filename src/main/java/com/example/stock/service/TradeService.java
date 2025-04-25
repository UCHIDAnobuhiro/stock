package com.example.stock.service;

import java.math.BigDecimal;

import jakarta.validation.Valid;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.example.stock.model.Trade;
import com.example.stock.model.UserWallet;
import com.example.stock.repository.TradeRepository;
import com.example.stock.util.TradeValidationUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 注文の保存やビジネスロジックを担当するサービスクラス。
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor // finalなフィールドを自動でコンストラクタに注入
public class TradeService {

	private final TradeRepository tradeRepository;
	private final UserWalletService userWalletService;
	private final StockService stockService;

	/**
	 * 注文データを保存する。
	 *
	 * @param dto フロントエンドから送られた注文情報
	 * @return 保存されたTradeエンティティ
	 */
	public Trade saveTrade(@Valid Trade newTrade) {
		return tradeRepository.save(newTrade);
	}

	/**
	 * 注文内容の検証を行う（残高チェック＋価格制限チェック）
	 * 問題がある場合は IllegalStateException をスロー。
	 *
	 * @param trade 対象取引
	 */
	public void validateTrade(Trade trade) {
		UserWallet wallet = userWalletService.getWalletByUser(trade.getUser());

		// 残高チェック
		if (!TradeValidationUtil.isBalanceEnough(trade, wallet)) {
			throw new IllegalStateException("残高不足です。注文を修正してください。");
		}

		// 価格制限チェック
		if (!TradeValidationUtil.isWithinLimit(trade, stockService)) {
			BigDecimal[] range = TradeValidationUtil.getPriceLimitRange(trade, stockService);
			throw new IllegalStateException(String.format(
					"注文価格が値幅制限を超えています（範囲: %s ～ %s）",
					range[0].toPlainString(), range[1].toPlainString()));
		}
	}
}
