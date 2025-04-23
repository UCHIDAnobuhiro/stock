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
@Table(name = "technical_indicator_value", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "symbol", "interval_type", "datetime", "indicator", "line_name", "period" })
})
@Getter
@Setter
@NoArgsConstructor
public class TechnicalIndicatorValue {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String symbol;
	@Column(name = "interval_type")
	private String interval; // "1day"など
	private LocalDateTime datetime;

	private String indicator; // "SMA", "BollingerBands", "Ichimoku", etc.
	private String lineName; // "sma", "upper", "tenkan_sen", "kijun_sen"など
	private Integer period; // 例: 5, 25, 75 など
	private Double value;

}
