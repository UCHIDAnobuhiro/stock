package com.example.stock.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.stock.exception.TickersException;
import com.example.stock.model.Tickers;
import com.example.stock.service.TickersService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class StockController {

	@Autowired
	private final TickersService tickersService;

	@GetMapping("/stock")
	public String stockPage(Model model) {
		try {
			List<Tickers> tickers = tickersService.getAllTickers();
			model.addAttribute("tickers", tickers);
		} catch (TickersException ex) {
			model.addAttribute(ex.getFieldName(), ex.getMessage());
		}
		return "stock";
	}

}
