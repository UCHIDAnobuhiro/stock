package com.example.stock.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StockController {
	@GetMapping("/stock")
	public String stockPage() {
		return "stock";
	}

}
