package com.example.stock.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.stock.model.StockCandle;

@Repository
public interface StockCandleRepository extends JpaRepository<StockCandle, Long> {

	// 重複登録防止のための検索
	Optional<StockCandle> findBySymbolAndIntervalAndDatetime(String symbol, String interval, LocalDateTime datetime);

	// 銘柄＆インターバルの最新データ取得用
	Page<StockCandle> findAllBySymbolAndIntervalOrderByDatetimeDesc(String symbol, String interval, Pageable pageable);

	List<StockCandle> findAllBySymbolAndIntervalAndDatetimeIn(String symbol, String interval,
			List<LocalDateTime> datetimes);
}