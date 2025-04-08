package com.example.stock.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StockCandleWithPrevCloseDto extends StockCandleDto {
	private double prevClose;
	private String symbol;
	private String interval;

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public StockCandleWithPrevCloseDto(String symbol, String interval, String datetime, double open, double high,
			double low,
			double close, long volume, double prevClose) {
		super(datetime, open, high, low, close, volume); // 親のコンストラクタ呼び出し
		this.prevClose = prevClose;
		this.symbol = symbol;
		this.interval = interval;
	}
}
