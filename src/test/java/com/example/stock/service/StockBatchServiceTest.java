package com.example.stock.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.example.stock.repository.TickersRepository;

@SpringBootTest
@ActiveProfiles("test")
public class StockBatchServiceTest {
	@Autowired
	private StockBatchService stockBatchService;

	@MockBean
	private StockService stockService;

	@MockBean
	private TechnicalService technicalService;

	@MockBean
	private TickersRepository tickersRepository;

	@MockBean
	private com.google.cloud.vision.v1.ImageAnnotatorClient mockVisionClient;

	@Test
	void testExecuteBatch_runsAllTasks() {
		// ダミーのtickerデータ
		when(tickersRepository.findAllTickers()).thenReturn(List.of("AAPL"));

		// バッチ本体を直接実行
		stockBatchService.runRateLimitedBatch();

		// 期待される呼び出し数の検証（Candle + SMA）
		verify(stockService, atLeastOnce()).saveStockCandles(anyString(), anyString(), anyInt());
		verify(technicalService, atLeastOnce()).fetchAndSaveSMA(anyString(), anyString(), anyInt(), anyInt());
	}

	@Test
	void testExecuteBatch_symbolError_shouldNotFailEntireBatch() {
		// "ERROR" のみを返すように設定（1件だけ）
		when(tickersRepository.findAllTickers()).thenReturn(List.of("ERROR"));

		// saveStockCandles が例外を投げるようにする
		doThrow(new RuntimeException("テスト用の例外"))
				.when(stockService).saveStockCandles(eq("ERROR"), anyString(), anyInt());

		// バッチ本体を直接実行
		stockBatchService.runRateLimitedBatch();

		// 例外が発生してもメソッドが呼ばれたことは確認
		verify(stockService, atLeastOnce()).saveStockCandles(eq("ERROR"), anyString(), anyInt());

		// fetchAndSaveSMA も呼ばれていることを確認（止まっていない証拠）
		verify(technicalService, atLeastOnce()).fetchAndSaveSMA(eq("ERROR"), anyString(), anyInt(), anyInt());
	}

}
