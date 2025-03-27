package com.example.stock.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.stock.dto.StockCandleDto;
import com.example.stock.dto.StockCandleWithPrevCloseDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class StockService {
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	// application.propertiesからAPIキーを読み込む。
	@Value("${alphavantage.api.key}")
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
	 * @return 完成したAPI URL
	 */
	private String buildTimeSeriesUrl(String symbol, String interval) {
		return UriComponentsBuilder
				.fromHttpUrl("https://api.twelvedata.com/time_series")
				.queryParam("symbol", symbol)
				.queryParam("interval", interval)
				.queryParam("outputsize", 100)
				.queryParam("apikey", apiKey)
				.toUriString();
	}

	/**
	 * 指定したシンボルとインターバルで株価の時系列データを取得する
	 *
	 * @param symbol 株のティッカーシンボル（例：AAPL）
	 * @param interval データの間隔（例：1day, 1week, 1month)
	 * @return Alpha Vantage API から返されたJSONデータ（文字列）
	 */
	public Map<String, Object> getStockTimeSeries(String symbol, String interval) {
		// Twelve Data APIからデータ取得
		String url = buildTimeSeriesUrl(symbol, interval);

		try {
			// APIへGETリクエストを送信し、レスポンスを取得
			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

			// JSON文字列をMap<String, Object>形式に変換して返す
			return objectMapper.readValue(response.getBody(), new TypeReference<>() {
			});
		} catch (Exception e) {
			throw new RuntimeException("JSONの変換に失敗しました", e);
		}
	}

	/**
	 * 指定された銘柄のローソク足データ（日足）に、前日の終値を追加したDTOのリストを取得します。
	 * データは古い順（昇順）に並んでいます。
	 *
	 * @param symbol 銘柄コード（例: "AAPL"）
	 * @return 前日終値付きのローソク足データのリスト
	 */
	public List<StockCandleWithPrevCloseDto> getStockWithPrevClose(String symbol) {
		// Twelve Data APIからデータ取得
		Map<String, Object> data = getStockTimeSeries(symbol, "1day");

		// valuesだけを取り出す
		List<Map<String, String>> raw = (List<Map<String, String>>) data.get("values");

		// 各データをStockCandleDtoに変換し、日付の昇順（古い順）にソート
		List<StockCandleDto> baseList = raw.stream()
				.map(v -> new StockCandleDto(
						v.get("datetime"),
						Double.parseDouble(v.get("open")),
						Double.parseDouble(v.get("high")),
						Double.parseDouble(v.get("low")),
						Double.parseDouble(v.get("close")),
						Long.parseLong(v.get("volume"))))
				.sorted(Comparator.comparing(StockCandleDto::getDatetime))
				.toList();

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
		return list.get(list.size() - 1); // 最新のデータ（リストは昇順）
	}

	/**
	 * 指定された銘柄とインターバル（日足・週足など）に基づいて、
	 * ローソク足形式の株価データ（日時、4本値、出来高）をDTOリストとして返却します。
	 *
	 * @param symbol 銘柄コード（例: "AAPL"）
	 * @param interval データの間隔（例: "1day", "1week", "1month"）
	 * @return 株価データのリスト（StockCandleDto形式）
	 */
	public List<StockCandleDto> getStockCandleDtoList(String symbol, String interval) {
		// Twelve Data APIから指定された銘柄・間隔の時系列データを取得
		Map<String, Object> data = getStockTimeSeries(symbol, interval);
		// "values"キーに格納されたローソク足データを取り出す（List<Map<String, String>> 形式）
		List<Map<String, String>> values = (List<Map<String, String>>) data.get("values");

		// 各データをStockCandleDtoに変換し、リストとして返却
		return values.stream()
				.map(v -> new StockCandleDto(
						v.get("datetime"),
						Double.parseDouble(v.get("open")),
						Double.parseDouble(v.get("high")),
						Double.parseDouble(v.get("low")),
						Double.parseDouble(v.get("close")),
						Long.parseLong(v.get("volume"))))
				.toList();
	}

}
