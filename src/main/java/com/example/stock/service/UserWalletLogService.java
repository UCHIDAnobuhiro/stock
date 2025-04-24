package com.example.stock.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.stock.model.Trade;
import com.example.stock.model.UserWallet;
import com.example.stock.model.UserWalletLog;
import com.example.stock.repository.UserWalletLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserWalletLogService {

	private final UserWalletLogRepository userWalletLogRepository;

	/**
	 * 指定された取引とウォレットに基づき、ログを生成・保存する。
	 * 
	 * @param trade 取引情報（売買区分、通貨、金額など）
	 * @param wallet 処理対象のウォレット
	 */
	public void createAndSaveLog(Trade trade, UserWallet wallet) {
		// 整合性確認
		if (!trade.getUser().getId().equals(wallet.getUser().getId())) {
			throw new IllegalArgumentException("取引とウォレットの所有者が一致しません。");
		}

		String currency = trade.getSettlementCurrency();
		BigDecimal beforeBalance = getCurrentBalance(wallet, currency);
		BigDecimal changeAmount = trade.getSide() == 0
				? beforeBalance.negate().add(beforeBalance.subtract(trade.getTotalPrice())) // 買い → 減る
				: trade.getTotalPrice(); // 売り → 増える

		BigDecimal afterBalance = beforeBalance.add(changeAmount);

		UserWalletLog log = new UserWalletLog();
		log.setUserWallet(wallet);
		log.setTrade(trade);
		log.setCurrency(currency);
		log.setBeforeBalance(beforeBalance);
		log.setAfterBalance(afterBalance);
		log.setChangeAmount(changeAmount);
		log.setCreateAt(LocalDateTime.now());

		userWalletLogRepository.save(log);
	}

	private BigDecimal getCurrentBalance(UserWallet wallet, String currency) {
		if ("JPY".equalsIgnoreCase(currency)) {
			return wallet.getJpyBalance();
		} else if ("USD".equalsIgnoreCase(currency)) {
			return wallet.getUsdBalance();
		}
		throw new IllegalArgumentException("不正な通貨コード：" + currency);
	}
}
