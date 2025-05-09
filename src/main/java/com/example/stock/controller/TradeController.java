package com.example.stock.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.stock.model.Trade;
import com.example.stock.model.Users;
import com.example.stock.security.SecurityUtils;
import com.example.stock.service.TradeService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class TradeController {
	private final TradeService tradeService;
	private final SecurityUtils securityUtils;

	@GetMapping("/trade-log")
	public String showTradeLog(Model model) {
		Users user = securityUtils.getLoggedInUserOrThrow();
		List<Trade> tradeList = tradeService.getTradesByUser(user);
		model.addAttribute("trades", tradeList);
		return "trade-log";
	}

	/**
	 * 取引履歴の検索処理を行う。
	 * ユーザーのログイン情報を元に、期間と銘柄コードに基づいてフィルタリングされた取引リストを取得し、対応するテンプレートを返す。
	 *
	 * @param date 検索対象の期間（today, 1week, 1month, all）
	 * @param ticker 検索対象の銘柄コード（部分一致可）
	 * @param model 画面に表示するためのデータを保持するオブジェクト
	 * @return 結果を表示するHTMLフラグメント
	 */
	@GetMapping("/trade-log/search")
	public String searchTrades(
			@RequestParam(defaultValue = "all") String date,
			@RequestParam(defaultValue = "") String ticker,
			Model model) {

		// 現在ログイン中のユーザーを取得
		Users user = securityUtils.getLoggedInUserOrThrow();

		// ユーザー、日付、銘柄コードでフィルタされた取引リストを取得
		List<Trade> trades = tradeService.searchTrades(user, date, ticker);
		model.addAttribute("trades", trades);

		// 銘柄コードが英字以外を含む場合はエラーメッセージを返す
		if (!ticker.matches("^[a-zA-Z]*$")) {
			model.addAttribute("errorMessage", "銘柄コードは英字のみで入力してください");
			return "fragments/order/trade-log-show :: trade-table-fragment";
		}

		// 取引が存在しない場合のエラーメッセージ
		if (trades.isEmpty()) {
			model.addAttribute("errorMessage", "該当する取引履歴はありません");
		}

		// 取引情報のテーブル部分テンプレートを返す
		return "fragments/order/trade-log-show :: trade-table-fragment";
	}

}
