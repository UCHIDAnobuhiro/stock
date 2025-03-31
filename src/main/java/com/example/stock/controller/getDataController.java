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
	 * 指定した銘柄と期間の株価データ（時系列）を取得します
	 * 
	 * 例: /api/stocks/time-series?symbol=AAPL&interval=1day
	 *
	 * @param symbol ティッカーシンボル(例: AAPL)
	 * @param interval データ間隔(例: 1day, 1week 1month)
	 * @return APIから取得した株価データのJSONマップ
	 */
	@GetMapping("/time-series")
	public ResponseEntity<Map<String, Object>> getDailyStock(
			@RequestParam String symbol,
			@RequestParam String interval) {
		Map<String, Object> data = stockService.getStockTimeSeries(symbol, interval);
		return ResponseEntity.ok(data);
	}

	/**
	 * chart.jsなどで利用するための、整形済みローソク足データを取得します。
	 * 
	 * 例: /api/stocks/time-series/values?symbol=AAPL&interval=1day
	 *
	 * @param symbol ティッカーシンボル(例: AAPL)
	 * @param interval データ間隔(例: 1day, 1week 1month)
	 * @return 形済みのローソク足リスト
	 */
	@GetMapping("time-series/values")
	public ResponseEntity<List<StockCandleDto>> getFilteredTimeSeries(
			@RequestParam String symbol,
			@RequestParam String interval) {

		List<StockCandleDto> candles = stockService.getStockCandleDtoList(symbol, interval);

		return ResponseEntity.ok(candles);

	}

}
