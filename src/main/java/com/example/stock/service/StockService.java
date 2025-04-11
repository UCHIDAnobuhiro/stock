package com.example.stock.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.stock.converter.StockCandleConverter;
import com.example.stock.dto.StockCandleWithPrevCloseDto;
import com.example.stock.exception.StockApiException;
import com.example.stock.model.StockCandle;
import com.example.stock.repository.StockCandleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockService {
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final StockCandleRepository stockCandleRepository;
	private final StockCandleConverter stockCandleConverter;

	private static final Logger logger = LoggerFactory.getLogger(StockService.class);

	// application.propertiesからAPIキーを読み込む。
	@Value("${api.key}")
	private String apiKey;

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
	 * 指定された銘柄・間隔・出力数に基づいて、Twelve Data APIからローソク足データを取得し、
	 * 各データに前日終値（prevClose）と銘柄シンボル（symbol）を付与したDTOのリストを返します。
	 *
	 * @param symbol     銘柄コード（例: "AAPL"）
	 * @param interval   データの間隔（例: "1day", "1week"）
	 * @param outputsize 取得するデータの件数（例: 30）
	 * @return StockCandleWithPrevCloseDtoのリスト（新しい順に並んでいる）
	 * @throws StockApiException APIレスポンスに異常がある場合（nullやパースエラーなど）
	 */
	public List<StockCandleWithPrevCloseDto> getStockCandleWithPrevCloseDtoList(String symbol, String interval,
			Integer outputsize) {
		// Twelve Data APIからデータ取得
		Map<String, Object> data = getStockTimeSeries(symbol, interval, outputsize);
		List<Map<String, String>> values = (List<Map<String, String>>) data.get("values");

		// 2. データが存在しない場合は例外をスロー
		if (values == null || values.isEmpty()) {
			throw new StockApiException("APIから株価データが取得できませんでした（valuesが空）");
		}

		// 「前日終値」を紐づけるため、古い順に並び替え
		Collections.reverse(values);

		List<StockCandleWithPrevCloseDto> dtoList = new ArrayList<>();
		Double prevClose = 0.0;

		// 各データをDTOに変換し、前日終値を付加
		for (Map<String, String> v : values) {
			try {
				double open = Double.parseDouble(v.get("open"));
				double high = Double.parseDouble(v.get("high"));
				double low = Double.parseDouble(v.get("low"));
				double close = Double.parseDouble(v.get("close"));
				long volume = Long.parseLong(v.get("volume"));

				// 1件分のローソク足データと前日終値をDTOに詰める
				StockCandleWithPrevCloseDto dto = new StockCandleWithPrevCloseDto(
						symbol,
						interval,
						v.get("datetime"),
						open,
						high,
						low,
						close,
						volume,
						prevClose);

				dtoList.add(dto);

				// 次のループ用に現在の終値を保存（これが次の「前日終値」になる）
				prevClose = close;
			} catch (NumberFormatException e) {
				logger.error("数値変換エラー: {}", v, e);
				throw new StockApiException("数値の変換に失敗しました：不正なデータがあります", e);
			}
		}
		// 結果リストを「新しい順」に戻して返却
		Collections.reverse(dtoList);

		return dtoList;
	}

	//前営業日の取得
	private LocalDate getPreviousBusinessDay(LocalDate base) {
		DayOfWeek day = base.getDayOfWeek();

		if (day == DayOfWeek.MONDAY) {
			return base.minusDays(3); // → 金曜日
		} else if (day == DayOfWeek.SUNDAY) {
			return base.minusDays(2); // → 金曜日
		} else if (day == DayOfWeek.SATURDAY) {
			return base.minusDays(1); // → 金曜日
		} else {
			return base.minusDays(1); // 平日なら前日でOK
		}
	}

	/**
	 * 指定された銘柄の最新のローソク足データ（前日終値付き）を取得します。
	 *
	 * @param symbol 銘柄コード（例: "AAPL"）
	 * @return 最新のローソク足データ（前日終値付き）
	 */
	public StockCandleWithPrevCloseDto getLatestStockWithPrevClose(String symbol) {
		String interval = "1day";
		LocalDate targetDate = getPreviousBusinessDay(LocalDate.now());
		LocalDateTime datetime = targetDate.atStartOfDay();

		Optional<StockCandle> candleOpt = stockCandleRepository
				.findBySymbolAndIntervalAndDatetime(symbol, interval, datetime);

		if (candleOpt.isPresent()) {
			return stockCandleConverter.fromEntity(candleOpt.get());
		}

		List<StockCandleWithPrevCloseDto> list = getStockCandleWithPrevCloseDtoList(symbol, "1day", 100);

		if (list.isEmpty()) {
			logger.warn("symbol={} のデータが空です（前日終値付き）", symbol);
			throw new StockApiException("最新の株価データが存在しませんでした");
		}
		saveStockCandles(symbol, interval, 200);
		return list.get(0); // 最新のデータ（リストは昇順）
	}

	/**
	 * 指定された銘柄（symbol）、時間足（interval）、出力サイズ（outputsize）に基づいて、
	 * 株価ローソク足データ（前日終値付き）を取得し、データベースに保存します。
	 * 既に同じ日時のデータが存在する場合は保存をスキップします。
	 *
	 * このメソッドはトランザクション内で実行され、途中で例外が発生した場合はロールバックされます。
	 *
	 * @param symbol     銘柄コード（例：AAPL、GOOGLなど）
	 * @param interval   時間足の種類（例：1min、5min、1dayなど）
	 * @param outputsize 取得するローソク足データの件数
	 */
	@Transactional
	public void saveStockCandles(String symbol, String interval, int outputsize) {
		List<StockCandleWithPrevCloseDto> dtoList = getStockCandleWithPrevCloseDtoList(symbol, interval, outputsize);

		for (StockCandleWithPrevCloseDto dto : dtoList) {
			LocalDateTime datetime = LocalDate.parse(dto.getDatetime()).atStartOfDay();
			boolean exists = stockCandleRepository
					.findBySymbolAndIntervalAndDatetime(symbol, interval, datetime)
					.isPresent();

			if (!exists) {
				stockCandleRepository.save(stockCandleConverter.toEntity(dto));
			}
		}
	}

	/**
	 * 指定されたシンボルとインターバルに対応するローソク足データを最新のものから指定件数分取得します。
	 *
	 * @param symbol    株式のシンボル（例: AAPL）
	 * @param interval  データの時間間隔（例: 1min、5min、1day など）
	 * @param outputsize 取得するローソク足データの件数
	 * @return 指定条件に一致する最新のローソク足データのリスト
	 */
	// @Cacheable(value = "candlesCache", key = "#symbol + ':' + #interval + ':' + #outputsize")
	public List<StockCandle> getSavedCandles(String symbol, String interval, int outputsize) {
		System.out.println(
				"データベースにアクセスしています... symbol=" + symbol + ", interval=" + interval + ", outputsize=" + outputsize);
		Pageable pageable = PageRequest.of(0, outputsize);
		Page<StockCandle> page = stockCandleRepository.findAllBySymbolAndIntervalOrderByDatetimeDesc(symbol, interval,
				pageable);
		List<StockCandle> candles = page.getContent();
		return candles;
	}

}
