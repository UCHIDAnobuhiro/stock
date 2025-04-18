package com.example.stock.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.stock.dto.OrderPageDataDto;
import com.example.stock.service.OrderPageDataService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class OrderController {

	private final OrderPageDataService orderPageDataService;

	@GetMapping("/stock/order")
	public String showOrderPage(@RequestParam String orderType, @RequestParam String symbol, Model model) {

		// symbol が無効なら stock に戻す
		if (symbol == null || symbol.trim().isEmpty()) {
			return "stock";
		}

		// Service からまとめてデータ取得
		OrderPageDataDto data = orderPageDataService.getOrderPageData(symbol);

		if (data == null) {
			return "stock"; // 取得に失敗したら stock ページへ
		}

		model.addAttribute("stock", data.getStock());
		model.addAttribute("userName", data.getUser().getDisplayName());
		model.addAttribute("jpyBalance", data.getJpyBalance());
		model.addAttribute("usdBalance", data.getUsdBalance());
		model.addAttribute("quantity", data.getQuantity());
		model.addAttribute("ticker", data.getTicker());

		model.addAttribute("orderType", orderType);

		return "order";
	}

}
