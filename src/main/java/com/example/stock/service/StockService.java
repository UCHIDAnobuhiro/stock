package com.example.stock.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.stock.dto.StockCandleDto;
import com.example.stock.dto.StockCandleWithPrevCloseDto;
import com.example.stock.exception.StockApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class StockService {
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	private static final Logger logger = LoggerFactory.getLogger(StockService.class);

	// application.propertiesからAPIキーを読み込む。
	@Value("${api.key}")
	private String apiKey;

	public StockService(RestTemplate restTemplate, ObjectMapper objectMapper) {
		this.restTemplate = restTemplate;
		this.objectMapper = objectMapper;
	}

	/**
	 * Twelve Data API のURLを構築する
	 *
	 * @param symbol   株のティッカーシンボル
	 * @param interval データの間隔
	 * @param outputsize データ数or ロウソク足の本数
	 * @return 完成したAPI URL
	 */
	private String buildTimeSeriesUrl(String symbol, String interval, Integer outputsize) {
		return UriComponentsBuilder.newInstance()
				.scheme("https")
				.host("api.twelvedata.com")
				.path("/time_series")
				.queryParam("symbol", symbol)
				.queryParam("interval", interval)
				.queryParam("outputsize", outputsize)
				.queryParam("apikey", apiKey)
				.toUriString();
	}

	/**
	 * 指定したシンボルとインターバルで株価の時系列データを取得する
	 *
	 * @param symbol 株のティッカーシンボル（例：AAPL）
	 * @param interval データの間隔（例：1day, 1week, 1month)
	 * @param outputsize データ数or ロウソク足の本数 (例: 50, 100) min:1 max:5000
	 *
	 * @return API から返されたJSONデータ（文字列）
	 */
	public Map<String, Object> getStockTimeSeries(String symbol, String interval, Integer outputsize) {
		// Twelve Data APIからデータ取得
		String url = buildTimeSeriesUrl(symbol, interval, outputsize);

		try {
			logger.info("Fetching stock data from API: {}", url);
			// APIへGETリクエストを送信し、レスポンスを取得
			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

			// JSON文字列をMap<String, Object>形式に変換して返す
			return objectMapper.readValue(response.getBody(), new TypeReference<>() {
			});
		} catch (Exception e) {
			logger.error("API取得失敗: {}", e.getMessage(), e);
			throw new StockApiException("株価のデータの取得に失敗しました", e);
		}
	}

	/**
	 * 指定された銘柄とインターバル（日足・週足など）に基づいて、
	 * ローソク足形式の株価データ（日時、4本値、出来高）をDTOリストとして返却します。
	 *
	 * @param symbol 銘柄コード（例: "AAPL"）
	 * @param interval データの間隔（例: "1day", "1week", "1month"）
	 * @param outputsize データ数or ロウソク足の本数 (例: 50, 100) min:1 max:5000
	 * 
	 * @return 株価データのリスト（StockCandleDto形式）
	 */
	public List<StockCandleDto> getStockCandleDtoList(String symbol, String interval, Integer outputsize) {
		// Twelve Data APIから指定された銘柄・間隔の時系列データを取得
		Map<String, Object> data = getStockTimeSeries(symbol, interval, outputsize);
		// "values"キーに格納されたローソク足データを取り出す（List<Map<String, String>> 形式）
		List<Map<String, String>> values = (List<Map<String, String>>) data.get("values");
		if (values == null || values.isEmpty()) {
			throw new StockApiException("APIから株価データが取得できませんでした（valuesが空）");
		}

		// 各データをStockCandleDtoに変換し、リストとして返却
		return values.stream()
				.map(v -> {
					try {
						return new StockCandleDto(
								v.get("datetime"),
								Double.parseDouble(v.get("open")),
								Double.parseDouble(v.get("high")),
								Double.parseDouble(v.get("low")),
								Double.parseDouble(v.get("close")),
								Long.parseLong(v.get("volume")));
					} catch (NumberFormatException e) {
						logger.error("数値変換エラー: {}", v, e);
						throw new StockApiException("数値の変換に失敗しました：不正なデータがあります", e);
					}
				})
				.toList();
	}

	/**
	 * 指定された銘柄のローソク足データ（日足）に、前日の終値を追加したDTOのリストを取得します。
	 * データは古い順（昇順）に並んでいます。
	 *
	 * @param symbol 銘柄コード（例: "AAPL"）
	 * 
	 * @return 前日終値付きのローソク足データのリスト
	 */
	public List<StockCandleWithPrevCloseDto> getStockWithPrevClose(String symbol) {
		// Twelve Data APIからデータ取得
		Map<String, Object> data = getStockTimeSeries(symbol, "1day", 2);

		// valuesだけを取り出す
		List<Map<String, String>> raw = (List<Map<String, String>>) data.get("values");
		if (raw == null || raw.isEmpty()) {
			logger.warn("取得されたデータが空でした（symbol={}）", symbol);
			throw new StockApiException("APIから取得した株価データが空、または無効です（前日終値が取得できません）");
		}

		// 各データをStockCandleDtoに変換し、日付の昇順（古い順）にソート
		List<StockCandleDto> baseList = new ArrayList<>();
		try {
			for (Map<String, String> v : raw) {
				baseList.add(new StockCandleDto(
						v.get("datetime"),
						Double.parseDouble(v.get("open")),
						Double.parseDouble(v.get("high")),
						Double.parseDouble(v.get("low")),
						Double.parseDouble(v.get("close")),
						Long.parseLong(v.get("volume"))));
			}
		} catch (NumberFormatException e) {
			logger.error("数値変換エラー:", e);
			throw new StockApiException("株価データの数値変換に失敗しました（不正な値が含まれている可能性）", e);
		}

		// 前日終値を付加したDTOリストを作成
		List<StockCandleWithPrevCloseDto> result = new ArrayList<>();
		for (int i = 0; i < baseList.size(); i++) {
			StockCandleDto current = baseList.get(i);
			double prevClose = (i > 0) ? baseList.get(i - 1).getClose() : current.getClose();

			result.add(new StockCandleWithPrevCloseDto(
					symbol,
					current.getDatetime(),
					current.getOpen(),
					current.getHigh(),
					current.getLow(),
					current.getClose(),
					current.getVolume(),
					prevClose));
		}

		return result;

	}

	/**
	 * 指定された銘柄の最新のローソク足データ（前日終値付き）を取得します。
	 *
	 * @param symbol 銘柄コード（例: "AAPL"）
	 * @return 最新のローソク足データ（前日終値付き）
	 */
	public StockCandleWithPrevCloseDto getLatestStockWithPrevClose(String symbol) {
		List<StockCandleWithPrevCloseDto> list = getStockWithPrevClose(symbol);

		if (list.isEmpty()) {
			logger.warn("symbol={} のデータが空です（前日終値付き）", symbol);
			throw new StockApiException("最新の株価データが存在しませんでした");
		}
		return list.get(list.size() - 1); // 最新のデータ（リストは昇順）
	}

}
