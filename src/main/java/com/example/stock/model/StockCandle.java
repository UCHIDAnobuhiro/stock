package com.example.stock.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_candle", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "symbol", "interval_type", "datetime" })
})
@Getter
@Setter
@NoArgsConstructor
public class StockCandle {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String symbol; // ← 銘柄コード（例: "AAPL"）
	@Column(name = "interval_type")
	private String interval; // ← "1day", "1week", "1month" など
	private LocalDateTime datetime;

	private double open;
	private double high;
	private double low;
	private double close;
	private long volume;

	@Column(name = "previous_close")
	private double previousClose;

}
