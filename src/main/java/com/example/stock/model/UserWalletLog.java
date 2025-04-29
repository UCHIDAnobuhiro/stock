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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Data;

@Entity
@Table(name = "user_wallet_log", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "user_wallet_id", "trade_id" })
})
@Data
public class UserWalletLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "user_wallet_id", nullable = false)
	private UserWallet userWallet;

	@ManyToOne(optional = false)
	@JoinColumn(name = "trade_id", nullable = false)
	private Trade trade;

	@Column(name = "currency", length = 3, nullable = false)
	private String currency;

	@Column(name = "before_balance", precision = 18, scale = 2, nullable = false)
	private BigDecimal beforeBalance;

	@Column(name = "after_balance", precision = 18, scale = 2, nullable = false)
	private BigDecimal afterBalance;

	@Column(name = "change_amount", precision = 18, scale = 2, nullable = false)
	private BigDecimal changeAmount;

	@Column(name = "create_at", nullable = false)
	private LocalDateTime createAt;
}
