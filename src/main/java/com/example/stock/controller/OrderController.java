package com.example.stock.controller;

import java.lang.reflect.Field;

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
		model.addAttribute("userName", data.getUser().getDisplayName());
		model.addAttribute("jpyBalance", data.getJpyBalance());
		model.addAttribute("usdBalance", data.getUsdBalance());
		model.addAttribute("quantity", data.getQuantity());
		model.addAttribute("ticker", data.getTicker());

		model.addAttribute("orderType", orderType);

		return "order";
	}

	private boolean hasNullField(Object dto) {
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
