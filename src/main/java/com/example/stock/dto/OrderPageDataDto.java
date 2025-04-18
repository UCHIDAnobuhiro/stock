package com.example.stock.dto;

import java.math.BigDecimal;

import com.example.stock.model.Tickers;
import com.example.stock.model.Users;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 注文ページで表示する必要な情報をまとめたDTO
 */
@Getter
@AllArgsConstructor
public class OrderPageDataDto {
	private Users user;
	private Tickers ticker;
	private StockCandleWithPrevCloseDto stock;
	private BigDecimal jpyBalance;
	private BigDecimal usdBalance;
	private BigDecimal quantity;
}
