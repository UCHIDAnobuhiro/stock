package com.example.stock.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.stock.model.TechnicalIndicatorValue;

@Repository
public interface TechnicalIndicatorValueRepository extends JpaRepository<TechnicalIndicatorValue, Long> {
	List<TechnicalIndicatorValue> findAllBySymbolAndIntervalAndIndicatorAndLineNameAndPeriodOrderByDatetimeDesc(
			String symbol, String interval, String indicator, String lineName, Integer period, Pageable pageable);

	List<TechnicalIndicatorValue> findBySymbolAndIntervalAndPeriodAndIndicator(
			String symbol,
			String interval,
			int period,
			String indicator);
}
