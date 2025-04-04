package com.example.stock.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.stock.dto.StockCandleDto;
import com.example.stock.exception.StockApiException;
import com.example.stock.service.StockService;

@RestController
@RequestMapping("/api/stocks")
public class GetDataController {
	private final StockService stockService;

	public GetDataController(StockService stockService) {
		this.stockService = stockService;
	}

	/**
	 * 指定した銘柄と期間の株価データ（時系列）を取得します
	 * 
	 * 例: /api/stocks/time-series?symbol=AAPL&interval=1day
	 *
	 * @param symbol ティッカーシンボル(例: AAPL)
	 * @param interval データ間隔(例: 1day, 1week 1month)
	 * @param outputsize データ数or ロウソク足の本数 (例: 50, 100) min:1 max:5000
	 * 
	 * @return APIから取得した株価データのJSONマップ
	 */
	@GetMapping("/time-series")
	public ResponseEntity<Map<String, Object>> getDailyStock(
			@RequestParam String symbol,
			@RequestParam String interval,
			@RequestParam Integer outputsize) {
		Map<String, Object> data = stockService.getStockTimeSeries(symbol, interval, outputsize);
		return ResponseEntity.ok(data);
	}

	/**
	 * chart.jsなどで利用するための、整形済みローソク足データを取得します。
	 * 
	 * 例: /api/stocks/time-series/values?symbol=AAPL&interval=1day
	 *
	 * @param symbol ティッカーシンボル(例: AAPL)
	 * @param interval データ間隔(例: 1day, 1week 1month)
	 * @param outputsize データ数or ロウソク足の本数 (例: 50, 100) min:1 max:5000
	 * 
	 * @return 形済みのローソク足リスト
	 */
	@GetMapping("/time-series/values")
	public ResponseEntity<?> getFilteredTimeSeries(
			@RequestParam String symbol,
			@RequestParam String interval,
			@RequestParam Integer outputsize) {
		try {
			List<StockCandleDto> candles = stockService.getStockCandleDtoList(symbol, interval, outputsize);
			return ResponseEntity.ok(candles);
		} catch (StockApiException e) {
			return ResponseEntity.status(502).body(Map.of(
					"error", "データ取得エラー",
					"message", e.getMessage()));
		}

	}

}
