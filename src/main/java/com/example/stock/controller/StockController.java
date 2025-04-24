package com.example.stock.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.stock.converter.TickersDTOConverter;
import com.example.stock.dto.StockCandleWithPrevCloseDto;
import com.example.stock.dto.TickersWithFavoriteDTO;
import com.example.stock.exception.StockApiException;
import com.example.stock.exception.TickersException;
import com.example.stock.model.Favorites;
import com.example.stock.model.Tickers;
import com.example.stock.model.Users;
import com.example.stock.security.SecurityUtils;
import com.example.stock.service.FavoritesService;
import com.example.stock.service.OrderPageDataService;
import com.example.stock.service.StockService;
import com.example.stock.service.TickersService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class StockController {

	private final TickersService tickersService;
	private final FavoritesService favoritesService;
	private final StockService stockService;
	private final SecurityUtils securityUtils;
	private final OrderPageDataService orderPageDataService;

	//stock.htmlを最初にallのtickersリストを表示
	@GetMapping("/stock")
	public String stockPage(Model model) {
		try {
			//基本情報を取得
			Users user = securityUtils.getLoggedInUserOrThrow();
			List<Tickers> tickers = tickersService.getAllTickers();
			List<Favorites> favorites = favoritesService.findFavoritesByUsers(user);

			// 銘柄の当日の情報を取得
			String symbol = "AAPL";
			StockCandleWithPrevCloseDto latest = stockService.getLatestStockWithPrevClose(symbol);
			Tickers ticker = tickersService.getTickersBySymbol(symbol);
			model.addAttribute("stock", latest);
			model.addAttribute("ticker", ticker);

			//tickersにisFavoriteを追加し、チェックボックスに使用される
			List<TickersWithFavoriteDTO> tickersWithFavoriteDTOs = TickersDTOConverter
					.convertToTickersWithFavoriteDTO(tickers, favorites);
			model.addAttribute("tickers", tickersWithFavoriteDTOs);
			model.addAttribute("favorites", favorites);
		} catch (TickersException ex) {
			model.addAttribute(ex.getFieldName(), ex.getMessage());
		}
		return "stock";
	}

	//すべてとお気に入りボタンを押下時のリスト変換
	@GetMapping("/stock-list")
	public String updateTickers(@RequestParam String show, Model model) {
		//TODO:エラーハンドリング
		Users user = securityUtils.getLoggedInUserOrThrow();

		//defaultはすべてのtickersを取得
		List<Tickers> tickers = tickersService.getAllTickers();
		List<Favorites> favorites = favoritesService.findFavoritesByUsers(user);

		if ("favorite".equals(show)) {
			//favoriteの場合はfavoriteに対応するtickersに変更
			tickers = tickersService.getFavoriteTickersByUser(user);
		}

		List<TickersWithFavoriteDTO> tickersWithFavoriteDTOs = TickersDTOConverter
				.convertToTickersWithFavoriteDTO(tickers, favorites);

		//tickersをあげ直す。一部の画面だけを変更する
		model.addAttribute("tickers", tickersWithFavoriteDTOs);

		//一部の画面だけを変更する
		return "fragments/stock/stock-show.html :: stocksDetailsTR";
	}

	@PatchMapping("/update-favorites")
	public ResponseEntity<String> updateFavorites(@RequestParam boolean isFavorite,
			@RequestParam Long tickerId, Model model) {
		try {
			Tickers ticker = tickersService.getTickerById(tickerId);
			Users user = securityUtils.getLoggedInUserOrThrow();
			// FAVORITEを更新
			if (isFavorite) {
				favoritesService.addFavorite(user, ticker);
			} else {
				favoritesService.deleteFavorite(user, ticker);
			}

			return ResponseEntity.ok("Favorite updated successfully");
		} catch (Exception e) {
			// 他の異常を処理
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("An error occurred while updating favorite: " + e.getMessage());
		}
	}

	@GetMapping("/stock/table")
	public String showStockTable(@RequestParam String symbol, Model model) {
		try {
			StockCandleWithPrevCloseDto latest = stockService.getLatestStockWithPrevClose(symbol);
			Tickers ticker = tickersService.getTickersBySymbol(symbol);
			model.addAttribute("stock", latest);
			model.addAttribute("ticker", ticker);
			return "fragments/stock/today-information :: today-information-template";

		} catch (StockApiException e) {
			model.addAttribute("errorMessage", "エラーが発生しました: " + e.getMessage());
			return "error";
		}
	}

}
