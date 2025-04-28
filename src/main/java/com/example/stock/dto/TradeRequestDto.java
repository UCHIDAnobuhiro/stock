package com.example.stock.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * フロントエンドからの注文データを受け取るDTOクラス。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeRequestDto {

	@NotNull(message = "銘柄IDは必須です。")
	@Positive(message = "銘柄IDは正の数である必要があります。")
	private Long tickerId;

	@NotNull(message = "数量は必須です。")
	@Positive(message = "数量は正の数を入力してください")
	private BigDecimal quantity;

	@NotNull(message = "価格は必須です。")
	@DecimalMin(value = "0.01", message = "価格は0.01以上を入力してください")
	private BigDecimal unitPrice;

	@NotNull(message = "決済通貨は必須です。")
	private String settlementCurrency;

	@NotNull(message = "為替レートは必須です。")
	@DecimalMin(value = "0.01", message = "為替レートは0.01以上を入力してください。")
	private BigDecimal exchangeRate;

	@NotNull(message = "売買区分は必須です。")
	@Pattern(regexp = "buy|sell", message = "売買区分が不正です。")
	private String side;

	@NotNull(message = "注文タイプは必須です。")
	@Pattern(regexp = "LIMIT|MARKET", message = "注文タイプが不正です。")
	private String type;

	private String tradingPin;

}
