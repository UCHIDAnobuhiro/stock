package com.example.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // getter/setter, toString, equals, hashCode など全部自動生成
@AllArgsConstructor // 全フィールド付きのコンストラクタ
@NoArgsConstructor // 引数なしコンストラクタ
public class StockCandleDto {
	private String datetime;
	private double open;
	private double high;
	private double low;
	private double close;
	private long volume;
}
