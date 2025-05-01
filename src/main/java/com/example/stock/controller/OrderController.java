package com.example.stock.controller;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import jakarta.servlet.http.HttpSession;
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
import com.example.stock.repository.TradeRepository;
import com.example.stock.security.SecurityUtils;
import com.example.stock.service.OrderPageDataService;
import com.example.stock.service.TickersService;
import com.example.stock.service.TradeService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class OrderController {

	private final OrderPageDataService orderPageDataService;
	private final TradeService tradeService;
	private final TickersService tickersService;
	private final TradeConverter tradeConverter;
	private final PasswordEncoder passwordEncoder;
	private final SecurityUtils securityUtils;
	private final TradeRepository tradeRepository;

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

		//デフォルトtradeを作成
		TradeRequestDto trade = new TradeRequestDto();
		trade.setQuantity(BigDecimal.ONE);
		trade.setSettlementCurrency("JPY");
		trade.setType("LIMIT");

		model.addAttribute("trade", trade);
		model.addAttribute("stock", data.getStock());
		model.addAttribute("ticker", data.getTicker());
		model.addAttribute("data", data);
		model.addAttribute("orderType", orderType);
		return "order";
	}

	/**
	 * 注文確認処理（POST）
	 * @param dto フォームからの入力
	 * @param result バリデーション結果
	 * @param model ビュー描画用モデル
	 * @return リダイレクト or 入力画面
	 */
	@PostMapping("/stock/order/submit")
	public String showOrderCheckPage(@Valid @ModelAttribute TradeRequestDto dto, BindingResult result, Model model,
			HttpSession session) {
		Users user = securityUtils.getLoggedInUserOrThrow();

		String symbol = tickersService.getTickerById(dto.getTickerId()).getTicker();
		OrderPageDataDto data = orderPageDataService.getOrderPageData(symbol);

		// 入力エラー
		if (result.hasErrors()) {
			StringBuilder errorMessages = new StringBuilder();
			result.getFieldErrors().forEach(error -> {
				errorMessages.append(error.getDefaultMessage());
			});
			model.addAttribute("errorMessage", errorMessages.toString());
			return returnToOrderPage(model, data, dto);
		}

		Trade newTrade = tradeConverter.toTradeEntity(dto);
		boolean isTradeSuccess = true;

		// 画面移動に必要なデータが足りない場合はstockへ戻る
		if (data == null || hasNullField(data)) {
			if (data != null) {
				model.addAttribute("ticker", data.getTicker());
				model.addAttribute("stock", data.getStock());
			}
			return "stock";
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

		// エラーが発生するなら入力画面に戻る
		if (!isTradeSuccess) {
			return returnToOrderPage(model, data, dto);
		}

		// エラーがなければ、注文確定処理（DB保存）
		tradeService.executeTrade(newTrade);

		// tradeId を session に保存（確認画面で使用）
		session.setAttribute("confirmedTradeId", newTrade.getId());

		// GETにリダイレクト（リロード対応のため）
		return "redirect:/stock/order/check";
	}

	/**
	 * 注文確認画面の表示（GET）
	 * @param tradeId DB保存された注文ID
	 * @param model ビュー描画用モデル
	 * @return 注文確認画面
	 */
	@GetMapping("/stock/order/check")
	public String showOrderConfirmationPage(Model model, HttpSession session) {
		Users user = securityUtils.getLoggedInUserOrThrow();
		Long tradeId = (Long) session.getAttribute("confirmedTradeId");
		if (tradeId == null) {
			return "redirect:/stock";
		}

		//ログイン中のユーザーのデータであるかをチェック 
		Trade trade = tradeRepository.findById(tradeId).orElse(null);
		if (trade == null || !trade.getUser().getId().equals(user.getId())) {
			return "redirect:/stock"; // 他人のデータアクセス防止
		}

		model.addAttribute("trade", trade);
		OrderPageDataDto data = orderPageDataService.getOrderPageData(trade.getTicker().getTicker());
		model.addAttribute("data", data);
		model.addAttribute("ticker", data.getTicker());
		model.addAttribute("stock", data.getStock());
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

	//エラーになったときにorderページに戻るためのめぞ
	private String returnToOrderPage(Model model, OrderPageDataDto data, TradeRequestDto dto) {
		model.addAttribute("stock", data.getStock());
		model.addAttribute("ticker", data.getTicker());
		model.addAttribute("data", data);
		model.addAttribute("orderType", dto.getSide());
		model.addAttribute("trade", dto);
		return "order";
	}

}