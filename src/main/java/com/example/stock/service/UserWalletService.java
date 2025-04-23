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
		wallet.setCreateAt(now); // 作成日時
		wallet.setUpdateAt(now); // 更新日時

		return userWalletRepository.save(wallet); // DBへ保存
	}

	public void applyTradeToWallet(Trade trade) {
		UserWallet wallet = getWalletByUser(trade.getUser());
		BigDecimal amount = trade.getTotalPrice();
		String currency = trade.getSettlementCurrency();

		// 売買区分: 0 = 買い（出金）, 1 = 売り（入金）
		if (trade.getSide() == 0) {
			// 買いの場合 → ユーザー残高から引く
			if ("JPY".equalsIgnoreCase(currency)) {
				wallet.setJpyBalance(wallet.getJpyBalance().subtract(amount));
			} else if ("USD".equalsIgnoreCase(currency)) {
				wallet.setUsdBalance(wallet.getUsdBalance().subtract(amount));
			}
		} else if (trade.getSide() == 1) {
			// 売りの場合 → ユーザー残高に加える
			if ("JPY".equalsIgnoreCase(currency)) {
				wallet.setJpyBalance(wallet.getJpyBalance().add(amount));
			} else if ("USD".equalsIgnoreCase(currency)) {
				wallet.setUsdBalance(wallet.getUsdBalance().add(amount));
			}
		}

		wallet.setUpdateAt(LocalDateTime.now());
		userWalletRepository.save(wallet);
	}

}