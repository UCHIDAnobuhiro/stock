package com.example.stock.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.stock.repository.TickersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockBatchService {
	private static final Logger log = LoggerFactory.getLogger(StockBatchService.class);

	private final StockService stockService;
	private final TechnicalService technicalService;
	private final TickersRepository tickersRepository;

	private static final List<String> INTERVALS = List.of("1day", "1week", "1month");

	private static final Map<String, List<Integer>> PERIODS_BY_INTERVAL = Map.of(
			"1day", List.of(5, 25, 75),
			"1week", List.of(13, 26, 52),
			"1month", List.of(9, 24, 60));

	@Scheduled(cron = "0 30 16  * * *")
	public void runRateLimitedBatch() {
		log.info("=== バッチ処理開始 ===");
		List<String> symbols = tickersRepository.findAllTickers();
		List<Runnable> tasks = new ArrayList<>();

		for (String symbol : symbols) {
			for (String interval : INTERVALS) {
				tasks.add(() -> {
					try {
						stockService.saveStockCandles(symbol, interval, 200);
						log.info("成功: {} / {}", symbol, interval);
					} catch (Exception e) {
						log.error("失敗: {} / {} -> {}", symbol, interval, e.getMessage(), e);
					}
				});

				for (int period : PERIODS_BY_INTERVAL.getOrDefault(interval, List.of())) {
					int p = period;
					tasks.add(() -> {
						try {
							technicalService.fetchAndSaveSMA(symbol, interval, p, 200);
							log.info("[SMA] 成功: {} / {} / period={}", symbol, interval, p);
						} catch (Exception e) {
							log.error("[SMA] 失敗: {} / {} / period={} -> {}", symbol, interval, p, e.getMessage(), e);
						}
					});
				}
			}
		}

		int count = 0;
		for (Runnable task : tasks) {
			if (count > 0 && count % 8 == 0) {
				log.info("💤 8件処理したので1分間待機...");
				try {
					Thread.sleep(60_000); // 1分待機
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					log.warn("待機中に中断されました", e);
				}
			}
			task.run();
			count++;
		}
		log.info("=== バッチ処理完了 ===");
	}
}