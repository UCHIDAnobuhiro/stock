package com.example.stock.service;

import static org.junit.jupiter.api.Assertions.*;
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
import com.fasterxml.jackson.databind.ObjectMapper;
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
	private ObjectMapper objectMapper;

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
	}

	// F-006-TC06	getSavedSMA の結果がキャッシュされることを検証
	@Test
	void getSavedSMA_shouldCacheResult_whenDataExists() {
		// 初回呼び出し（キャッシュされる）
		List<TechnicalIndicatorValue> first = technicalIndicatorService.getSavedSMA(symbol, interval, period,
				outputsize);
		assertNotNull(first);

		// 2回目（キャッシュから取得される）
		List<TechnicalIndicatorValue> second = technicalIndicatorService.getSavedSMA(symbol, interval, period,
				outputsize);

		// 呼び出し回数が1回のみであることを検証（キャッシュされている）
		verify(technicalIndicatorService, times(1)).getSavedSMA(symbol, interval, period, outputsize);
	}

	// F-006-TC07	getSavedSMA が空リストを返す場合はキャッシュされないことを検証
	@Test
	void getSavedSMA_shouldNotCache_whenResultIsEmpty() {
		// リポジトリをモックして空リストを返すように設定しておくこと（この例では割愛）
		List<TechnicalIndicatorValue> result = technicalIndicatorService.getSavedSMA("FAKE", interval, period,
				outputsize);
		assertTrue(result.isEmpty());

		// 2回目呼び出し（キャッシュされてないのでまた呼ばれる）
		List<TechnicalIndicatorValue> second = technicalIndicatorService.getSavedSMA("FAKE", interval, period,
				outputsize);
		verify(technicalIndicatorService, times(2)).getSavedSMA("FAKE", interval, period, outputsize);
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
