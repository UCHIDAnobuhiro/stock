package com.example.stock.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.stock.exception.TickersException;
import com.example.stock.model.Favorites;
import com.example.stock.model.Tickers;
import com.example.stock.model.Users;
import com.example.stock.service.FavoritesService;
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
	@Autowired
	private final FavoritesService favoritesService;

	@GetMapping("/stock")
	public String stockPage(Model model) {
		try {
			Users user = usersService.getLoggedInUser();
			List<Tickers> tickers = tickersService.getAllTickers();
			List<Favorites> favorites = favoritesService.findFavoritesByUsers(user);
			model.addAttribute("tickers", tickers);
			model.addAttribute("favorites", favorites);
		} catch (TickersException ex) {
			model.addAttribute(ex.getFieldName(), ex.getMessage());
		}
		return "stock";
	}

	//すべてとお気に入りボタンを押下時のリスト変換
	@PatchMapping("/stock")
	public String updateTickers(@RequestParam String show, Model model) {
		List<Tickers> tickers;
		if ("favorite".equals(show)) {
			Users user = usersService.getLoggedInUser();
			tickers = tickersService.getFavoriteTickersByUser(user);
		} else {
			tickers = tickersService.getAllTickers();
		}
		model.addAttribute("tickers", tickers);
		return "fragments/stock/stock-show.html :: stocksDetailsTR";
	}

	//各銘柄のお気に入りボタンを押下時のFAVORITESに追加と削除
	@PatchMapping("/updateFavorites")
	public ResponseEntity<String> updateFavorites(@RequestParam("isFavorite") boolean isFavorite,
			@RequestParam("tickerId") Long tickerId) {
		Tickers ticker = tickersService.getTickerById(tickerId);
		Users user = usersService.getLoggedInUser();
		if (isFavorite) {
			favoritesService.addFavorite(user, ticker);
		} else {
			favoritesService.deleteFavorite(user, ticker);
		}
		return ResponseEntity.ok("Favorite updated successfully");
	}

}
