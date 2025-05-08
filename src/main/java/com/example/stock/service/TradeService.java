package com.example.stock.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.example.stock.model.Trade;
import com.example.stock.model.UserWallet;
import com.example.stock.model.Users;
import com.example.stock.repository.TradeRepository;
import com.example.stock.util.TradeValidationUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 注文の保存や検証、残高/保有株の更新を一括管理するサービス
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class TradeService {

	private final TradeRepository tradeRepository;
	private final UserWalletService userWalletService;
	private final StockService stockService;
	private final UserStockService userStockService;

	/**
	 * 注文を実行する（検証 → 保存 → 更新）。途中でエラーがあれば自動ロールバック。
	 */
	@Transactional
	public void executeTrade(@Valid Trade trade) {
		try {
			log.info("【取引開始】ユーザーID: {}, 銘柄: {}, 数量: {}, 単価: {}, 通貨: {}, 側: {}",
					trade.getUser().getId(), trade.getTicker().getTicker(), trade.getQuantity(),
					trade.getUnitPrice(), trade.getSettlementCurrency(), trade.getSide());

			// 1. 初期検証
			validateTrade(trade);

			// 2. 取引保存
			tradeRepository.save(trade);
			log.info("【注文保存】成功：取引ID: {}", trade.getId());

			// 3. 最終検証（並行処理に備える）
			validateTrade(trade);

			// 4. ウォレット更新
			userWalletService.applyTradeToWallet(trade);

			// 5. 保有株更新
			userStockService.applyTradeToUserStock(trade);

			log.info("【取引完了】取引ID: {}", trade.getId());

		} catch (Exception e) {
			log.error("【取引失敗】取引ID: {}, エラー: {}", trade.getId(), e.getMessage(), e);
			throw e; // @Transactional によってロールバックされる
		}
	}

	/**
	 * 注文内容の検証を行う（残高チェック＋価格制限チェック）
	 * 問題がある場合は IllegalStateException をスロー。
	 *
	 * @param trade 対象取引
	 */
	public void validateTrade(Trade trade) {
		UserWallet wallet = userWalletService.getWalletByUser(trade.getUser());

		// 買い注文 → 残高チェック
		if (trade.getSide() == 0 && !TradeValidationUtil.isBalanceEnough(trade, wallet)) {
			throw new IllegalStateException("残高不足です。注文を修正してください。");
		}

		// 値幅チェック
		if (!TradeValidationUtil.isWithinLimit(trade, stockService)) {
			BigDecimal[] range = TradeValidationUtil.getPriceLimitRange(trade, stockService);
			throw new IllegalStateException(String.format(
					"注文価格が値幅制限を超えています（許容範囲: %s ～ %s）",
					range[0].toPlainString(), range[1].toPlainString()));
		}

		// 売り注文 → 保有数チェック
		if (trade.getSide() == 1 && !TradeValidationUtil.isSellQuantityEnough(trade, userStockService)) {
			throw new IllegalStateException("保有株数が不足です。注文を修正してください。");
		}
	}

	public List<Trade> getTradesByUser(Users user) {
		return tradeRepository.findByUserOrderByCreateAtDesc(user);
	}

	/**
	 * ユーザーの取引履歴を検索します。
	 *
	 * @param user ログイン中のユーザー
	 * @param date フィルター対象の日付範囲（"today", "1week", "1month", "all"）
	 * @param tickerKeyword ティッカーの検索キーワード（連続部分一致）
	 * @return 該当する取引リスト（新しい順）
	 */
	public List<Trade> searchTrades(Users user, String date, String tickerKeyword) {
		LocalDateTime from = switch (date) {
		case "today" -> LocalDate.now().atStartOfDay();
		case "1week" -> LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay();
		case "1month" -> LocalDate.now().withDayOfMonth(1).atStartOfDay();
		default -> LocalDateTime.of(2000, 1, 1, 0, 0); // "all"
		};

		return tradeRepository.findByUserAndCreateAtAfterOrderByCreateAtDesc(user, from).stream()
				.filter(t -> tickerKeyword == null || tickerKeyword.isBlank()
						|| t.getTicker().getTicker().toLowerCase().contains(tickerKeyword.toLowerCase()))
				.toList();
	}

}
