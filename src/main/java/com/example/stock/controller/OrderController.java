package com.example.stock.controller;

import java.lang.reflect.Field;

import jakarta.validation.Valid;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.stock.converter.TradeConverter;
import com.example.stock.dto.OrderPageDataDto;
import com.example.stock.dto.TradeRequestDto;
import com.example.stock.model.Trade;
import com.example.stock.model.Users;
import com.example.stock.security.SecurityUtils;
import com.example.stock.service.OrderPageDataService;
import com.example.stock.service.TickersService;
import com.example.stock.service.TradeService;
import com.example.stock.service.UserStockService;
import com.example.stock.service.UserWalletService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class OrderController {

	private final OrderPageDataService orderPageDataService;
	private final TradeService tradeService;
	private final TickersService tickersService;
	private final TradeConverter tradeConverter;
	private final UserWalletService userWalletService;
	private final UserStockService userStockService;
	private final PasswordEncoder passwordEncoder;
	private final SecurityUtils securityUtils;

	/**
	 * 注文ページの初期表示
	 * @param orderType "buy" or "sell"
	 * @param symbol 銘柄コード
	 * @param model 表示モデル
	 * @return 注文入力画面
	 */
	@GetMapping("/stock/order")
	public String showOrderPage(@RequestParam String orderType, @RequestParam String symbol, Model model) {
		OrderPageDataDto data = orderPageDataService.getOrderPageData(symbol);

		if (data == null || hasNullField(data)) {
			if (data != null) {
				model.addAttribute("ticker", data.getTicker());
				model.addAttribute("stock", data.getStock());
			}
			return "stock";
		}

		model.addAttribute("stock", data.getStock());
		model.addAttribute("ticker", data.getTicker());
		model.addAttribute("data", data);
		model.addAttribute("orderType", orderType);
		return "order";
	}

	/**
	 * 注文確認処理
	 * @param dto フォームからの入力
	 * @param result バリデーション結果
	 * @param model ビュー描画用モデル
	 * @return 注文確認画面または入力画面
	 */
	@PostMapping("/stock/order/submit")
	public String showOrderCheckPage(@Valid @ModelAttribute TradeRequestDto dto, BindingResult result, Model model) {

		String symbol = tickersService.getTickerById(dto.getTickerId()).getTicker();
		Trade newTrade = tradeConverter.toTradeEntity(dto);
		Users user = securityUtils.getLoggedInUserOrThrow();
		boolean isTradeSuccess = true;

		//画面移動に必要なデータが足りない場合はstockへ戻る
		OrderPageDataDto data = orderPageDataService.getOrderPageData(symbol);
		if (data == null || hasNullField(data)) {
			if (data != null) {
				model.addAttribute("ticker", data.getTicker());
				model.addAttribute("stock", data.getStock());
			}
			return "stock";
		}

		// 入力エラー
		if (result.hasErrors()) {
			isTradeSuccess = false;
			result.getFieldErrors().forEach(e -> System.out.println(e.getField() + ": " + e.getDefaultMessage()));
			model.addAttribute("errorMessage", "入力に誤りがあります");
		}

		// パスワード確認
		if (!passwordEncoder.matches(dto.getTradingPin(), user.getPassword())) {
			isTradeSuccess = false;
			model.addAttribute("errorMessage", "パスワードが正しくありません。");
		}

		// 業務バリデーション（残高・価格）
		if (isTradeSuccess) {
			try {
				tradeService.validateTrade(newTrade);
			} catch (IllegalStateException e) {
				isTradeSuccess = false;
				model.addAttribute("errorMessage", e.getMessage());
			}
		}

		//エラーが発生するなら
		if (!isTradeSuccess) {
			// 入力画面に必要なデータを送り、入力画面へ戻る
			model.addAttribute("stock", data.getStock());
			model.addAttribute("ticker", data.getTicker());
			model.addAttribute("data", data);
			model.addAttribute("orderType", dto.getSide());
			return "order";
		}

		// エラーが発生してないなら、注文確定処理（DB保存）
		tradeService.saveTrade(newTrade);
		userWalletService.applyTradeToWallet(newTrade);
		userStockService.applyTradeToUserStock(newTrade);

		// 最新データ再取得し確認画面へ遷移
		OrderPageDataDto updatedData = orderPageDataService.getOrderPageData(symbol);
		model.addAttribute("data", updatedData);
		return "order-check";
	}

	//dtoにnullがあるかをチェック
	private Boolean hasNullField(Object dto) {
		try {
			for (Field field : dto.getClass().getDeclaredFields()) {
				field.setAccessible(true);
				Object value = field.get(dto);
				if (value == null) {
					return true;
				}
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return false;
	}

}