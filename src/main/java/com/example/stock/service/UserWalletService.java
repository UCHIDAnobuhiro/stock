package com.example.stock.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.example.stock.model.Trade;
import com.example.stock.model.UserWallet;
import com.example.stock.model.Users;
import com.example.stock.repository.UserWalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserWalletService {
	private final UserWalletRepository userWalletRepository;
	private final UserWalletLogService userWalletLogService;
	//	private final UserStockService userStockService;

	/**
	 * 指定されたユーザーに対応するウォレットを取得します。
	 * ウォレットが存在しない場合は新規作成を試みます。
	 * ただし、並行リクエストによるユニーク制約違反を考慮し、例外をキャッチして再取得します。
	 *
	 * @param user 対象のユーザー
	 * @return 該当ユーザーのウォレット
	 */
	public UserWallet getWalletByUser(Users user) {
		UserWallet userWallet = userWalletRepository.findByUser(user);

		if (userWallet == null) {
			try {
				// ウォレットが存在しない場合、新しく作成する
				userWallet = createWalletForUser(user);
			} catch (DataIntegrityViolationException e) {
				// 別スレッド・リクエストが同時にウォレットを作成し、
				// 一意制約（user_idのUNIQUE）違反が発生する可能性がある
				// → その場合は再度DBからウォレットを取得する
				userWallet = userWalletRepository.findByUser(user);
			}
		}

		return userWallet;
	}

	/**
	 * 指定されたユーザーに対して新しいウォレットを作成します。
	 * 初期残高はJPY/USD共に0です。
	 *
	 * @param user 対象のユーザー
	 * @return 作成されたウォレットエンティティ
	 */
	public UserWallet createWalletForUser(Users user) {
		Objects.requireNonNull(user, "ユーザーがnullです。ウォレットを作成できません。");
		UserWallet wallet = new UserWallet();
		wallet.setUser(user);
		wallet.setJpyBalance(BigDecimal.ZERO); // 初期JPY残高
		wallet.setUsdBalance(BigDecimal.ZERO); // 初期USD残高

		LocalDateTime now = LocalDateTime.now();
		wallet.setCreateAt(now);
		wallet.setUpdateAt(now);

		return userWalletRepository.save(wallet); // DBへ保存
	}

	/**
	 * 取引に応じてウォレットの残高を更新。
	 * チェック失敗時はログのみ記録（画面へのエラー送出はしない）。
	 *
	 * @param trade 対象取引
	 */
	public void applyTradeToWallet(Trade trade) {
		UserWallet wallet = getWalletByUser(trade.getUser());
		BigDecimal amount = trade.getTotalPrice();
		String currency = trade.getSettlementCurrency();
		BigDecimal balance = "JPY".equalsIgnoreCase(currency) ? wallet.getJpyBalance() : wallet.getUsdBalance();

		// 通貨チェック
		if (!"JPY".equalsIgnoreCase(currency) && !"USD".equalsIgnoreCase(currency)) {
			log.error("【通貨エラー】取引ID：{}, ユーザーID: {}, 未対応の通貨: {}", trade.getId(), trade.getUser().getId(), currency);
			throw new IllegalStateException("未対応の通貨です: " + currency);
		}

		// 【買い注文】残高チェック（最終検証）
		if (trade.getSide() == 0) {
			if (balance.compareTo(amount) < 0) {
				log.error("【残高エラー】取引ID：{}, ユーザーID: {}, 通貨: {}, 必要金額: {}, 残高: {}",
						trade.getId(), trade.getUser().getId(), currency, amount, balance);
				throw new IllegalStateException("【最終検証】残高不足");
			}
		}

		// 残高の変更を行う
		if (trade.getSide() == 0) {
			if ("JPY".equalsIgnoreCase(currency)) {
				wallet.setJpyBalance(wallet.getJpyBalance().subtract(amount));
			} else {
				wallet.setUsdBalance(wallet.getUsdBalance().subtract(amount));
			}
		} else if (trade.getSide() == 1) {
			if ("JPY".equalsIgnoreCase(currency)) {
				wallet.setJpyBalance(wallet.getJpyBalance().add(amount));
			} else {
				wallet.setUsdBalance(wallet.getUsdBalance().add(amount));
			}
		}

		wallet.setUpdateAt(LocalDateTime.now());
		// DB保存
		userWalletRepository.save(wallet);
		// log作成・保存
		userWalletLogService.createAndSaveLog(trade, wallet);

		log.info("【口座情報更新】ユーザーID: {}, 通貨: {}, 処理数量: {}, 残り: {}",
				trade.getUser().getId(), currency, amount, balance);
	}

}