package com.example.stock.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.stock.converter.StockCandleConverter;
import com.example.stock.dto.StockCandleWithPrevCloseDto;
import com.example.stock.exception.StockApiException;
import com.example.stock.model.StockCandle;
import com.example.stock.service.StockService;
import com.example.stock.service.TechnicalService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class GetDataController {
	private final StockService stockService;
	private final TechnicalService technicalService;
	private final StockCandleConverter stockCandleConverter;

	/**
	 * データベースに保存されている株価ローソク足データを取得し、DTOに変換して返却します。
	 * 
	 * データ件数が {@code outputsize} に満たない場合は、外部API（Twelve Data）からデータを取得して
	 * データベースに保存した後、再度取得を試みます。
	 *
	 * @param symbol     銘柄コード（例: "AAPL"）。デフォルトは "AAPL"
	 * @param interval   データの時間間隔（例: "1day", "1week"）。デフォルトは "1day"
	 * @param outputsize 必要なデータ件数（例: 200）。デフォルトは 200
	 * @return 株価ローソク足データのリスト（JSON形式で返される）
	 *         - 正常：200 OK + データリスト
	 *         - データが取得できない場合：404 Not Found + エラーメッセージ
	 */
	@GetMapping("/list")
	public ResponseEntity<?> getSavedCandles(
			@RequestParam(defaultValue = "AAPL") String symbol,
			@RequestParam(defaultValue = "1day") String interval,
			@RequestParam(defaultValue = "200") int outputsize) {

		// データベースから取得
		List<StockCandle> candles = stockService.getSavedCandles(symbol, interval, outputsize);

		// データが不足している場合はAPIから補完して再取得
		if (candles.size() < outputsize) {
			stockService.saveStockCandles(symbol, interval, outputsize);
			candles = stockService.getSavedCandles(symbol, interval, outputsize);
			if (candles.isEmpty()) {
				return ResponseEntity.status(404).body(Map.of(
						"error", "データなし",
						"message", "指定された条件のデータが見つかりませんでした"));
			}
		}

		// DTO化して返す
		List<StockCandleWithPrevCloseDto> dtoList = candles.stream()
				.map(candle -> stockCandleConverter.fromEntity(candle))
				.toList();

		return ResponseEntity.ok(dtoList);
	}

	/**
	 * SMA（単純移動平均）テクニカル指標データを取得するAPIエンドポイント。
	 * 
	 * クエリパラメータとして株式シンボル、時間間隔、期間、出力サイズを受け取り、
	 * Twelve Data APIを通じてSMAデータを取得します。
	 *
	 * @param symbol 対象の株式シンボル（例：AAPL）
	 * @param interval データの時間間隔（例：1min, 5min, 1day など）
	 * @param timeperiod 移動平均を算出する期間（例：10, 20など）
	 * @param outputsize 出力されるデータの件数（例：30, 500など）
	 * @return SMAデータを含むHTTPレスポンス（成功時は200 OK, 失敗時は502 Bad Gateway）
	 */
	@GetMapping("/technical/SMA")
	public ResponseEntity<?> getSMA(
			@RequestParam(defaultValue = "AAPL") String symbol,
			@RequestParam(defaultValue = "1day") String interval,
			@RequestParam(defaultValue = "5") Integer timeperiod,
			@RequestParam(defaultValue = "200") Integer outputsize) {
		try {
			Map<String, Object> smaData = technicalService.getSMATechnicalIndicator(symbol, interval, timeperiod,
					outputsize);
			return (ResponseEntity.ok(smaData));
		} catch (StockApiException e) {
			return ResponseEntity.status(502).body(Map.of(
					"error", "データ取得エラー",
					"message", e.getMessage()));
		}
	}

}
