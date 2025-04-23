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

	@Column(name = "quantity", nullable = false, precision = 18, scale = 2)
	private BigDecimal quantity;

	@Column(name = "unit_price", precision = 18, scale = 2, nullable = false)
	private BigDecimal unitPrice;

	@Column(name = "total_price", precision = 18, scale = 2, nullable = false)
	private BigDecimal totalPrice;

	@Column(name = "currency", nullable = false, length = 3)
	private String currency;

	@Column(name = "settlement_currency", nullable = false, length = 3)
	private String settlementCurrency;

	@Column(name = "exchange_rate", precision = 18, scale = 2)
	private BigDecimal exchangeRate;

	@Column(name = "side", nullable = false, columnDefinition = "TINYINT UNSIGNED")
	private int side;

	@Column(name = "type", nullable = false, columnDefinition = "TINYINT UNSIGNED")
	private int type;

	@Column(name = "status", nullable = false, columnDefinition = "TINYINT UNSIGNED")
	private int status = 4;

	@Column(name = "create_at", nullable = false)
	private LocalDateTime createAt;

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
