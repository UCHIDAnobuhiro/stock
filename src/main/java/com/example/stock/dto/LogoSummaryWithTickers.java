package com.example.stock.dto;

import com.example.stock.model.Tickers;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LogoSummaryWithTickers {
	private Tickers tickerEntity;
	private String summaryHtml;
}
