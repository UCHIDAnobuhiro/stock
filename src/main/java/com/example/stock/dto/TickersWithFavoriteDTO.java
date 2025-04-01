package com.example.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TickersWithFavoriteDTO {

	private Long id;
	private String ticker;
	private String brand;
	private boolean isFavorite;
}
