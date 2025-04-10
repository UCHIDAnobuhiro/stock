package com.example.stock.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.stock.converter.TechnicalIndicatorConverter;
import com.example.stock.dto.FlexibleIndicatorDto;
import com.example.stock.exception.StockApiException;
import com.example.stock.model.TechnicalIndicatorValue;
import com.example.stock.repository.TechnicalIndicatorValueRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TechnicalService {
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final TechnicalIndicatorConverter technicalIndicatorConverter;
	private final TechnicalIndicatorValueRepository technicalIndicatorValueRepository;

	private static final Logger logger = LoggerFactory.getLogger(TechnicalService.class);

	// application.propertiesからAPIキーを読み込む。
	@Value("${api.key}")
	private String apiKey;

	/**
	 * 指定されたパラメータを元に、SMA（単純移動平均）テクニカル指標の取得用URLを構築します。
	 *
	 * @param symbol 株式のシンボル（例：AAPL）
	 * @param interval データの間隔（例：1min、5min、1dayなど）
	 * @param timePeriod 移動平均を計算する期間
	 * @param outputsize 出力されるデータのサイズ（例：30, 500など）
	 * @return SMAテクニカル指標を取得するためのURL文字列
	 */
	private String buildSMATechnicalUrl(String symbol, String interval,
			Integer timePeriod, Integer outputsize) {
		return UriComponentsBuilder.newInstance()
				.scheme("https")
				.host("api.twelvedata.com")
				.path("sma")
				.queryParam("symbol", symbol)
				.queryParam("interval", interval)
				.queryParam("time_period", timePeriod)
				.queryParam("outputsize", outputsize)
				.queryParam("apikey", apiKey)
				.toUriString();
	}

	/**
	 * SMA（単純移動平均）テクニカル指標のデータをTwelve Data APIから取得します。
	 *
	 * @param symbol 株式のシンボル（例：AAPL）
	 * @param interval データの間隔（例：1min、5min、1dayなど）
	 * @param timePeriod 移動平均を計算する期間
	 * @param outputsize 出力されるデータのサイズ（例：30, 500など）
	 * @return 取得したSMAテクニカル指標データ（Map形式）
	 * @throws StockApiException API呼び出し時にエラーが発生した場合にスローされます
	 */
	public Map<String, Object> getSMATechnicalIndicator(String symbol, String interval,
			int timePeriod, int outputsize) {
		// Twelve Data APIからデータ取得
		String url = buildSMATechnicalUrl(symbol, interval, timePeriod, outputsize);

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

	public void fetchAndSaveSMA(String symbol, String interval, int period, int outputsize) {
		try {
			// 1. APIからSMAデータ取得（Map形式）
			Map<String, Object> responseMap = getSMATechnicalIndicator(symbol, interval, period, outputsize);

			// 2. Map → JSON文字列に変換
			String json = objectMapper.writeValueAsString(responseMap);

			// 3. JSON → DTOリスト
			List<FlexibleIndicatorDto> dtoList = technicalIndicatorConverter.parseDtoFromJson(json);

			// 4. DTO → Entityリスト
			List<TechnicalIndicatorValue> entities = technicalIndicatorConverter.toEntities(symbol, interval, "SMA",
					period, dtoList);

			// 5. DB保存
			technicalIndicatorValueRepository.saveAll(entities);

		} catch (Exception e) {
			throw new RuntimeException("SMAデータの取得または保存に失敗しました", e);
		}
	}

	/**
	 * 指定された条件に一致するSMAテクニカル指標データを、最新のものから指定件数分取得します。
	 *
	 * @param symbol    株式のシンボル（例: AAPL）
	 * @param interval  データの時間間隔（例: 1day, 1min など）
	 * @param period    移動平均の期間（例: 25, 75 など）
	 * @param outputsize 取得するデータの件数
	 * @return 指定条件に一致するSMAのリスト
	 */
	@Cacheable(value = "smaCache", key = "#symbol + ':' + #interval + ':' + #period + ':' + #outputsize")
	public List<TechnicalIndicatorValue> getSavedSMA(String symbol, String interval, int period, int outputsize) {
		System.out.println(
				"SMAデータをDBから取得中: " + symbol + ", " + interval + ", period=" + period + ", size=" + outputsize);
		Pageable pageable = PageRequest.of(0, outputsize);
		return technicalIndicatorValueRepository
				.findAllBySymbolAndIntervalAndIndicatorAndLineNameAndPeriodOrderByDatetimeDesc(
						symbol, interval, "SMA", "sma", period, pageable);
	}

}
