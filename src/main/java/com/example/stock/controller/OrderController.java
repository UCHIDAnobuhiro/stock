package com.example.stock.controller;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.validation.Valid;

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

	@GetMapping("/stock/order")
	public String showOrderPage(@RequestParam String orderType, @RequestParam String symbol, Model model) {

		// Service からまとめてデータ取得
		OrderPageDataDto data = orderPageDataService.getOrderPageData(symbol);

		//accountデータ取得失敗の時
		if (data == null || hasNullField(data)) {
			if (data != null) {
				if (data.getTicker() != null)
					model.addAttribute("ticker", data.getTicker());
				if (data.getStock() != null)
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

	@PostMapping("/stock/order/submit")
	public String showOrderCheckPage(@Valid @ModelAttribute TradeRequestDto dto, BindingResult result, Model model) {

		String symbol = tickersService.getTickerById(dto.getTickerId()).getTicker();
		Trade newTrade = tradeConverter.toTradeEntity(dto);
		Boolean isTradeSucces = true;
		System.out.println(newTrade);

		// Service からまとめてデータ取得
		OrderPageDataDto data = orderPageDataService.getOrderPageData(symbol);

		//accountデータ取得失敗の時
		if (data == null || hasNullField(data)) {
			if (data != null) {
				if (data.getTicker() != null)
					model.addAttribute("ticker", data.getTicker());
				if (data.getStock() != null)
					model.addAttribute("stock", data.getStock());
			}
			return "stock";
		}

		//入力エラー
		if (result.hasErrors()) {
			isTradeSucces = false;
			result.getFieldErrors().forEach(e -> {
				System.out.println(e.getField() + ": " + e.getDefaultMessage());
			});
			model.addAttribute("errorMessage", "入力に誤りがあります");
		}

		//余力不足エラー
		if (!tradeService.isBalanceEnough(newTrade)) {
			isTradeSucces = false;
			model.addAttribute("errorMessage", "残高不足のため、再度注文を確認してください");
		}

		// 値幅制限エラー（現在値±10%超過）
		if (!tradeService.isWithinlimit(newTrade)) {
			isTradeSucces = false;

			//エラーメッセージ作成
			BigDecimal closePrice = BigDecimal.valueOf(data.getStock().getClose()).setScale(2, RoundingMode.HALF_UP);
			BigDecimal upperLimit = closePrice.multiply(BigDecimal.valueOf(1.1)).setScale(2, RoundingMode.HALF_UP);
			BigDecimal lowerLimit = closePrice.multiply(BigDecimal.valueOf(0.9)).setScale(2, RoundingMode.HALF_UP);
			String errorMessage = String.format(
					"注文価格が値幅制限を超えています（範囲: %s ～ %s）",
					lowerLimit.toPlainString(),
					upperLimit.toPlainString());
			model.addAttribute("errorMessage", errorMessage);
		}

		//画面移動
		if (isTradeSucces) {
			tradeService.saveTrade(newTrade);
			userWalletService.applyTradeToWallet(newTrade);
			userStockService.applyTradeToUserStock(newTrade);
			OrderPageDataDto updatedData = orderPageDataService.getOrderPageData(symbol);
			model.addAttribute("data", updatedData);
			return "order-check";
		} else {
			model.addAttribute("stock", data.getStock());
			model.addAttribute("ticker", data.getTicker());
			model.addAttribute("data", data);
			model.addAttribute("orderType", dto.getSide());
			return "order";
		}
	}

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