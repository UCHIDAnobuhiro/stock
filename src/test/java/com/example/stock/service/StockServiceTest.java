package com.example.stock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.test.context.ActiveProfiles;

import com.example.stock.exception.StockApiException;

@SpringBootTest
@ActiveProfiles("test")
@EnableCaching
public class StockServiceTest {
	@SpyBean
	private StockService stockService;

	@Autowired
	private CacheManager cacheManager;

	@BeforeEach
	void clearCache() {
		cacheManager.getCache("symbolCache").clear();
	}

	// F-006-TC01	getLatestStockWithPrevClose の結果がキャッシュされることを検証
	@Test
	void getLatestStockWithPrevClose_shouldUseCache() {
		String symbol = "AAPL";

		// 1回目（キャッシュされる）
		var first = stockService.getLatestStockWithPrevClose(symbol);

		// 2回目（キャッシュヒット）
		var second = stockService.getLatestStockWithPrevClose(symbol);

		assertEquals(first, second);
		verify(stockService, times(1)).getLatestStockWithPrevClose(symbol);
	}

	// F-006-TC02	例外発生時に getLatestStockWithPrevClose の結果がキャッシュされないことを検証
	@Test
	void getLatestStockWithPrevClose_shouldNotCacheWhenExceptionThrown() {
		String invalidSymbol = "INVALID";

		// 初回で例外をスローさせる（空リスト or Repositoryが空）
		assertThrows(StockApiException.class, () -> {
			stockService.getLatestStockWithPrevClose(invalidSymbol);
		});

		// キャッシュされていない（キャッシュにキーが存在しない）
		assertNull(cacheManager.getCache("symbolCache").get(invalidSymbol));
	}

	// F-006-TC03	getSavedCandles が正常に結果を返すとき、キャッシュされることを検証
	@Test
	void getSavedCandles_shouldUseCache_whenDataExists() {
		String symbol = "AAPL";
		String interval = "1day";
		int outputsize = 2;

		var first = stockService.getSavedCandles(symbol, interval, outputsize);
		var second = stockService.getSavedCandles(symbol, interval, outputsize);

		assertIterableEquals(first, second);
		verify(stockService, times(1)).getSavedCandles(symbol, interval, outputsize);
	}

	// F-006-TC04	getSavedCandles が空リストを返した場合、キャッシュされないことを検証
	@Test
	void getSavedCandles_shouldNotCache_whenResultIsEmpty() {
		String symbol = "NOSUCH";
		String interval = "1day";
		int outputsize = 2;

		var result = stockService.getSavedCandles(symbol, interval, outputsize);
		assertTrue(result.isEmpty());

		// キャッシュされていない（unlessによって）
		String key = symbol + ":" + interval + ":" + outputsize;
		assertNull(cacheManager.getCache("candlesCache").get(key));
	}

	// F-006-TC05	saveStockCandles 実行後に、関連するキャッシュが削除されることを検証
	@Test
	void saveStockCandles_shouldEvictCaches() {
		String symbol = "AAPL";
		String interval = "1day";
		int outputsize = 2;

		// キャッシュさせる
		stockService.getLatestStockWithPrevClose(symbol);
		stockService.getSavedCandles(symbol, interval, outputsize);

		// キャッシュがあることを確認
		assertNotNull(cacheManager.getCache("symbolCache").get(symbol));
		assertNotNull(cacheManager.getCache("candlesCache").get(symbol + ":1day:2"));

		// キャッシュを削除する操作を実行
		stockService.saveStockCandles(symbol, "1day", 2);

		// キャッシュが削除されたか確認
		assertNull(cacheManager.getCache("symbolCache").get(symbol));
		assertNull(cacheManager.getCache("candlesCache").get(symbol + ":1day:2"));
	}

}
