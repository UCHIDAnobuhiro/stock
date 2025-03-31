package com.example.stock.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "favorites")
@Getter
@Setter
public class Favorites {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false) // Usersテーブルのidを参照する外部キー
	private Users user; // `Users`エンティティとの関連付け

	@ManyToOne
	@JoinColumn(name = "ticker_id", nullable = false) // Usersテーブルのidを参照する外部キー
	private Tickers ticker; // `Users`エンティティとの関連付け

	@Column(name = "create_at", nullable = false)
	private LocalDateTime createAt;

}