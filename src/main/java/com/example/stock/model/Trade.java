package com.example.stock.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Entity
@Table(name = "trade")
@Data
public class Trade {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private Users user;

	@ManyToOne(optional = false)
	@JoinColumn(name = "ticker_id", nullable = false)
	private Tickers ticker;

	/** 株数（必須 / 0.01以上） */
	@NotNull(message = "株数は必須です")
	@DecimalMin(value = "0.01", message = "株数は0.01以上である必要があります")
	@Column(name = "quantity", nullable = false, precision = 18, scale = 2)
	private BigDecimal quantity;

	/** 単価（必須 / 0.01以上） */
	@NotNull(message = "単価は必須です")
	@DecimalMin(value = "0.01", message = "単価は0.01以上である必要があります")
	@Column(name = "unit_price", precision = 18, scale = 2, nullable = false)
	private BigDecimal unitPrice;

	/** 合計金額（必須 / 0.01以上） */
	@NotNull(message = "合計金額は必須です")
	@DecimalMin(value = "0.01", message = "合計金額は0.01以上である必要があります")
	@Column(name = "total_price", precision = 18, scale = 2, nullable = false)
	private BigDecimal totalPrice;

	/** 通貨（必須 / JPYまたはUSD） */
	@NotBlank(message = "通貨は必須です")
	@Size(max = 3, message = "通貨コードは3文字以内で入力してください")
	@Column(name = "currency", nullable = false, length = 3)
	private String currency;

	/** 決済通貨（必須 / JPYまたはUSD） */
	@NotBlank(message = "決済通貨は必須です")
	@Size(max = 3, message = "通貨コードは3文字以内で入力してください")
	@Column(name = "settlement_currency", nullable = false, length = 3)
	private String settlementCurrency;

	/** 為替レート（任意だが、指定時は0.01以上） */
	@DecimalMin(value = "0.01", message = "為替レートは0.01以上である必要があります")
	@Column(name = "exchange_rate", precision = 18, scale = 2)
	private BigDecimal exchangeRate;

	/** 売買区分（0=買い, 1=売り） */
	@Min(value = 0, message = "売買区分が不正です")
	@Max(value = 1, message = "売買区分が不正です")
	@Column(name = "side", nullable = false, columnDefinition = "TINYINT UNSIGNED")
	private int side;

	/** 注文タイプ（0=指値, 1=成行） */
	@Min(value = 0, message = "注文タイプが不正です")
	@Max(value = 1, message = "注文タイプが不正です")
	@Column(name = "type", nullable = false, columnDefinition = "TINYINT UNSIGNED")
	private int type;

	/** ステータス（初期値：4） */
	@Column(name = "status", nullable = false, columnDefinition = "TINYINT UNSIGNED")
	private int status = 4;

	/** 登録日時 */
	@NotNull
	@Column(name = "create_at", nullable = false)
	private LocalDateTime createAt;

	/** 更新日時 */
	@NotNull
	@Column(name = "update_at", nullable = false)
	private LocalDateTime updateAt;

	@PrePersist
	protected void onCreate() {
		this.createAt = this.updateAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		this.updateAt = LocalDateTime.now();
	}
}
