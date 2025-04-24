package com.example.stock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import com.example.stock.converter.TechnicalIndicatorConverter;
import com.example.stock.model.TechnicalIndicatorValue;
import com.example.stock.repository.TechnicalIndicatorValueRepository;
import com.google.cloud.vision.v1.ImageAnnotatorClient;

@SpringBootTest
@ActiveProfiles("test")
@EnableCaching
public class TechnicalServiceTest {
	@SpyBean
	private TechnicalService technicalIndicatorService;

	@MockBean
	private RestTemplate restTemplate;

	@MockBean
	private TechnicalIndicatorConverter technicalIndicatorConverter;

	@MockBean
	private TechnicalIndicatorValueRepository technicalIndicatorValueRepository;

	@MockBean
	private ImageAnnotatorClient imageAnnotatorClient;

	@Autowired
	private CacheManager cacheManager;

	private final String symbol = "AAPL";
	private final String interval = "1day";
	private final int period = 25;
	private final int outputsize = 10;

	private String cacheKey;

	@BeforeEach
	void setUp() {
		cacheKey = symbol + ":" + interval + ":" + period + ":" + outputsize;
		// キャッシュクリア
		cacheManager.getCache("smaCache").clear();

		TechnicalIndicatorValue dummy = new TechnicalIndicatorValue();

		// 正常系の戻り値（AAPL）
		when(technicalIndicatorValueRepository
				.findAllBySymbolAndIntervalAndIndicatorAndLineNameAndPeriodOrderByDatetimeDesc(
						eq(symbol), eq(interval), eq("SMA"), eq("sma"), eq(period), any()))
								.thenReturn(List.of(dummy));

		// 異常系の戻り値（FAKE）
		when(technicalIndicatorValueRepository
				.findAllBySymbolAndIntervalAndIndicatorAndLineNameAndPeriodOrderByDatetimeDesc(
						eq("FAKE"), eq(interval), eq("SMA"), eq("sma"), eq(period), any()))
								.thenReturn(List.of());
	}

	// F-006-TC06	getSavedSMA の結果がキャッシュされることを検証
	@Test
	void getSavedSMA_shouldCacheResult_whenDataExists() {
		// 初回呼び出し（キャッシュされる）
		List<TechnicalIndicatorValue> first = technicalIndicatorService.getSavedSMA(symbol, interval, period,
				outputsize);
		assertNotNull(first);

		// キャッシュから取得されるか確認
		Cache cache = cacheManager.getCache("smaCache");
		assertNotNull(cache.get(cacheKey)); // キャッシュが存在するか確認
	}

	// F-006-TC07	getSavedSMA が空リストを返す場合はキャッシュされないことを検証
	@Test
	void getSavedSMA_shouldNotCache_whenResultIsEmpty() {
		// リポジトリをモックして空リストを返すように設定しておくこと（この例では割愛）
		List<TechnicalIndicatorValue> result = technicalIndicatorService.getSavedSMA("FAKE", interval, period,
				outputsize);
		assertTrue(result.isEmpty());

		Cache cache = cacheManager.getCache("smaCache");
		String fakeKey = "FAKE:1day:25:10";
		assertNull(cache.get(fakeKey)); // キャッシュされていないことを確認

	}

	// F-006-TC08	fetchAndSaveSMA 実行時に smaCache の該当エントリが削除されることを検証
	@Test
	void fetchAndSaveSMA_shouldEvictCache() {
		// 1回キャッシュしておく
		technicalIndicatorService.getSavedSMA(symbol, interval, period, outputsize);
		Cache cache = cacheManager.getCache("smaCache");
		assertNotNull(cache.get(cacheKey)); // キャッシュ存在確認

		// キャッシュを削除
		technicalIndicatorService.fetchAndSaveSMA(symbol, interval, period, outputsize);
		assertNull(cache.get(cacheKey)); // キャッシュが削除されていることを確認
	}

}
