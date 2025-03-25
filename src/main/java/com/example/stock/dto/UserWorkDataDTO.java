package com.example.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserWorkDataDTO {

	private String name;
	private String workTimeMonth;
	private Integer workDaysMonth;
	private Integer month;
}
