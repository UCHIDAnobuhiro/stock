package com.example.stock.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "user_wallet")
@Data
public class UserWallet {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne
	@JoinColumn(name = "user_id", nullable = false) // Usersテーブルのidを参照する外部キー
	private Users user; // `Users`エンティティとの関連付け

	@Column(name = "balance", nullable = false)
	private BigDecimal balance;

	@Column(name = "update_at", nullable = false)
	private LocalDateTime updateAt;
}