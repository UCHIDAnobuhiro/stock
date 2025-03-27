package com.example.stock.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.stock.dto.StockCandleDto;
import com.example.stock.service.StockService;

@RestController
@RequestMapping("/api/stocks")
public class getDataController {
	private final StockService stockService;

	public getDataController(StockService stockService) {
		this.stockService = stockService;
	}

	/**
	 * 株価データ（日足）を取得するAPIエンドポイント
	 * 例: /api/stocks/time-series?symbol=AAPL&interval=1day
	 *
	 * @param symbol interval(1day, 1week 1month) クエリパラメータで受け取るティッカーシンボル
	 * @return APIからのレスポンス（JSON文字列）
	 */
	@GetMapping("/time-series")
	public ResponseEntity<Map<String, Object>> getDailyStock(@RequestParam String symbol,
			@RequestParam String interval) {
		Map<String, Object> data = stockService.getStockTimeSeries(symbol, interval);
		return ResponseEntity.ok(data);
	}

	/**
	 * 日付、4本値、出来高をchart.jsに渡すためのAPIエンドポイント
	 * 例: /api/stocks/time-series/values?symbol=AAPL&interval=1day
	 *
	 * @param symbol interval(1day, 1week 1month) クエリパラメータで受け取るティッカーシンボル
	 * @return APIからのレスポンス（JSON文字列）
	 */
	@GetMapping("time-series/values")
	public ResponseEntity<List<StockCandleDto>> getFilteredTimeSeries(
			@RequestParam String symbol,
			@RequestParam String interval) {
		Map<String, Object> data = stockService.getStockTimeSeries(symbol, interval);

		// valuesをList<Map<String, String>>として取得
		List<Map<String, String>> values = (List<Map<String, String>>) data.get("values");

		List<StockCandleDto> filtered = values.stream()
				.map(v -> new StockCandleDto(
						v.get("datetime"),
						Double.parseDouble(v.get("open")),
						Double.parseDouble(v.get("high")),
						Double.parseDouble(v.get("low")),
						Double.parseDouble(v.get("close")),
						Long.parseLong(v.get("volume"))))
				.toList();

		return ResponseEntity.ok(filtered);

	}

}
