package com.example.stock.converter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.example.stock.dto.StockCandleWithPrevCloseDto;
import com.example.stock.model.StockCandle;

@Component
public class StockCandleConverter {
	/**
	 * DTO（StockCandleWithPrevCloseDto）からエンティティ（StockCandle）への変換を行います。
	 * 日付は文字列から {@link LocalDateTime} に変換され、時間は00:00として設定されます。
	 *
	 * @param dto 前日終値付きの株価ローソク足データDTO
	 * @return エンティティ形式の {@link StockCandle} オブジェクト
	 */
	public static StockCandle toEntity(StockCandleWithPrevCloseDto dto) {
		StockCandle entity = new StockCandle();
		entity.setSymbol(dto.getSymbol());
		entity.setInterval(dto.getInterval()); // "1day" など
		entity.setDatetime(LocalDate.parse(dto.getDatetime()).atStartOfDay());
		entity.setOpen(dto.getOpen());
		entity.setHigh(dto.getHigh());
		entity.setLow(dto.getLow());
		entity.setClose(dto.getClose());
		entity.setVolume(dto.getVolume());
		entity.setPreviousClose(dto.getPrevClose());
		return entity;
	}

	/**
	 * エンティティ {@link StockCandle} を表示・通信用の DTO {@link StockCandleWithPrevCloseDto} に変換します。
	 * 
	 * 日付は {@link LocalDateTime} から "yyyy-MM-dd" 形式の文字列に変換されます。
	 * また、前日終値（prevClose）は呼び出し元で計算・指定された値を使用します。
	 *
	 * @param candle    データベースから取得したローソク足エンティティ
	 * @param prevClose 前日終値（比較表示などに使用される）
	 * @return 表示・通信向けに整形された DTO オブジェクト
	 */
	public static StockCandleWithPrevCloseDto fromEntity(StockCandle candle) {
		return new StockCandleWithPrevCloseDto(
				candle.getSymbol(),
				candle.getInterval(),
				candle.getDatetime().toLocalDate().toString(), // "yyyy-MM-dd" に変換
				candle.getOpen(),
				candle.getHigh(),
				candle.getLow(),
				candle.getClose(),
				candle.getVolume(),
				candle.getPreviousClose());
	}

}
