package com.example.stock.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.example.stock.model.UserWallet;
import com.example.stock.model.Users;
import com.example.stock.repository.UserWalletRepository;

import lombok.RequiredArgsConstructor;

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
		UserWallet wallet = new UserWallet();
		wallet.setUser(user);
		wallet.setJpyBalance(BigDecimal.ZERO); // 初期JPY残高
		wallet.setUsdBalance(BigDecimal.ZERO); // 初期USD残高

		LocalDateTime now = LocalDateTime.now();
		wallet.setCreateAt(now); // 作成日時
		wallet.setUpdateAt(now); // 更新日時

		return userWalletRepository.save(wallet); // DBへ保存
	}
}