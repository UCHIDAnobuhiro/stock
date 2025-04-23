package com.example.stock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.stock.converter.TradeConverter;
import com.example.stock.model.Trade;
import com.example.stock.model.UserWallet;
import com.example.stock.repository.TradeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 注文の保存やビジネスロジックを担当するサービスクラス。
 */
@Slf4j
@Service
@RequiredArgsConstructor // finalなフィールドを自動でコンストラクタに注入
public class TradeService {

	private static final Logger logger = LoggerFactory.getLogger(TradeService.class);
	private final TradeRepository tradeRepository;
	private final TradeConverter tradeConverter;
	private final UserWalletService userWalletService;
	private final StockService stockService;

	/**
	 * 注文データを保存する。
	 *
	 * @param dto フロントエンドから送られた注文情報
	 * @return 保存されたTradeエンティティ
	 */
	public Trade saveTrade(Trade newTrade) {
		return tradeRepository.save(newTrade);
	}

	public Boolean isBalanceEnough(Trade trade) {
		if (trade.getSide() != 0) {
			return true; // 売り注文は残高チェック不要
		}

		UserWallet wallet = userWalletService.getWalletByUser(trade.getUser());
		String currency = trade.getSettlementCurrency();

		BigDecimal balance = "JPY".equalsIgnoreCase(currency)
				? wallet.getJpyBalance()
				: wallet.getUsdBalance();

		boolean result = balance.compareTo(trade.getTotalPrice()) >= 0;

		log.info("【残高チェック】ユーザーID: {}, 通貨: {}, 現在残高: {}, 必要金額: {}, 判定: {}",
				trade.getUser().getId(),
				currency,
				balance.toPlainString(),
				trade.getTotalPrice().toPlainString(),
				result ? "OK ✅" : "NG ❌");

		return result;
	}

	public Boolean isWithinlimit(Trade trade) {
		String symbol = trade.getTicker().getTicker();
		BigDecimal closePrice = BigDecimal
				.valueOf(stockService.getLatestStockWithPrevClose(symbol).getClose())
				.setScale(2, RoundingMode.HALF_UP);

		BigDecimal upper = closePrice.multiply(BigDecimal.valueOf(1.1)).setScale(2, RoundingMode.HALF_UP);
		BigDecimal lower = closePrice.multiply(BigDecimal.valueOf(0.9)).setScale(2, RoundingMode.HALF_UP);
		BigDecimal unitPrice = trade.getUnitPrice();

		Boolean result = unitPrice.compareTo(upper) <= 0 && unitPrice.compareTo(lower) >= 0;

		return result;
	}

	//	/**
	//	 * ビジネスルールに基づいた入力チェック
	//	 *
	//	 * @param dto TradeRequestDto
	//	 */
	//	private void validateRequest(TradeRequestDto dto) {
	//		if (dto.getUserId() == null || dto.getUserId() <= 0) {
	//			throw new IllegalArgumentException("無効なユーザーIDです。");
	//		}
	//		if (dto.getTickerId() == null || dto.getTickerId() <= 0) {
	//			throw new IllegalArgumentException("無効な銘柄IDです。");
	//		}
	//		if (dto.getQuantity() == null || dto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
	//			throw new IllegalArgumentException("数量は1以上である必要があります。");
	//		}
	//		if (dto.getType() == 0 && (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0)) {
	//			throw new IllegalArgumentException("指値注文の場合、価格を指定してください。");
	//		}
	//		if (dto.getType() == 1 && dto.getPrice() != null) {
	//			throw new IllegalArgumentException("成行注文では価格を指定しないでください。");
	//		}
	//		if (dto.getSide() != 0 && dto.getSide() != 1) {
	//			throw new IllegalArgumentException("売買区分が不正です。");
	//		}
	//		if (dto.getType() != 0 && dto.getType() != 1) {
	//			throw new IllegalArgumentException("注文タイプが不正です。");
	//		}
	//	}
}
