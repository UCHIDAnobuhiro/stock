package com.example.stock.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.stock.exception.TickersException;
import com.example.stock.model.Tickers;
import com.example.stock.model.Users;
import com.example.stock.service.TickersService;
import com.example.stock.service.UsersService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class StockController {

	@Autowired
	private final TickersService tickersService;
	@Autowired
	private final UsersService usersService;

	@GetMapping("/stock")
	public String stockPage(Model model) {
		try {
			Users user = usersService.getLoggedInUser();
			List<Tickers> tickers = tickersService.getFavoriteTickersByUser(user);
			model.addAttribute("tickers", tickers);
		} catch (TickersException ex) {
			model.addAttribute(ex.getFieldName(), ex.getMessage());
		}
		return "stock";
	}

	//	@GetMapping("/stock")
	//	public String stockPage(Model model) {
	//		try {
	//			List<Tickers> tickers = tickersService.getAllTickers();
	//			model.addAttribute("tickers", tickers);
	//		} catch (TickersException ex) {
	//			model.addAttribute(ex.getFieldName(), ex.getMessage());
	//		}
	//		return "stock";
	//	}
}
