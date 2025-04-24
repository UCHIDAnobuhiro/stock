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
import com.example.stock.util.TradeValidationUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserWalletService {
	private final UserWalletRepository userWalletRepository;
	private final StockService stockService;
	private final UserWalletLogService userWalletLogService;

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
		boolean isAvailable = true;

		// チェック：残高・価格制限・通貨
		if (trade.getSide() == 0) { // 買い（残高減少）
			if (!TradeValidationUtil.isBalanceEnough(trade, wallet)) {
				log.error("【残高エラー】取引ID：{}, ユーザーID: {}, 通貨: {}, 必要金額: {}, 残高: {}",
						trade.getId(), trade.getUser().getId(), currency, amount,
						"JPY".equalsIgnoreCase(currency) ? wallet.getJpyBalance() : wallet.getUsdBalance());
				isAvailable = false;
			}
			if (!TradeValidationUtil.isWithinLimit(trade, stockService)) {
				BigDecimal[] range = TradeValidationUtil.getPriceLimitRange(trade, stockService);
				log.error("【価格制限エラー】取引ID：{}, ユーザーID: {}, 単価: {}, 許容範囲: {} ～ {}",
						trade.getId(), trade.getUser().getId(), trade.getUnitPrice(),
						range[0], range[1]);
				isAvailable = false;
			}
		} else if (trade.getSide() == 1) {
			// 売り侧はチェック不要（今の仕様では無条件で加算可能）
		}

		// 通貨チェック
		if (!"JPY".equalsIgnoreCase(currency) && !"USD".equalsIgnoreCase(currency)) {
			log.error("【通貨エラー】取引ID：{}, ユーザーID: {}, 未対応の通貨: {}", trade.getId(), trade.getUser().getId(), currency);
			isAvailable = false;
		}

		//チェックに問題があるなら、そのままreturn
		if (!isAvailable) {
			System.out.println("出现问题，不更新订单");
			return;
		}

		// チェックをすべて通過後、残高の変更を行う
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

		// log作成・保存
		userWalletLogService.createAndSaveLog(trade, wallet);

		System.out.println("订单正常，保存数据");
		// DB保存
		userWalletRepository.save(wallet);
	}

}