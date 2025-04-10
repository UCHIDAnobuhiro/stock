package com.example.stock.dto;

import java.util.Map;

import lombok.Data;

@Data
public class FlexibleIndicatorDto {
	private String datetime;
	private Map<String, String> indicators;
}
