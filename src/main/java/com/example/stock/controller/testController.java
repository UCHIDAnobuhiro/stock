package com.example.stock.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.stock.dto.StockCandleWithPrevCloseDto;
import com.example.stock.service.StockService;

@Controller
public class testController {
	private final StockService stockService;

	public testController(StockService stockService) {
		this.stockService = stockService;
	}

	// /stock/table?symbol=AAPLでアクセするとAPPLEの株価データ取得できます。
	@GetMapping("/stock/table")
	public String showStockTable(@RequestParam String symbol, Model model) {

		// 最新の情報だけ抽出
		StockCandleWithPrevCloseDto latest = stockService.getLatestStockWithPrevClose(symbol);

		model.addAttribute("stock", latest); // ← 1件だけ渡す

		return "test";
	}
}
