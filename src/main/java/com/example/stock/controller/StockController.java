package com.example.stock.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.stock.service.TickersService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class StockController {

	@Autowired
	private final TickersService tickersService;

	@GetMapping("/stock")
	public String stockPage(Model model) {
		tickersService.getAllTickers();
		return "stock";
	}

}
