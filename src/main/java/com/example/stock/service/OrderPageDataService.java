package com.example.stock.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.example.stock.dto.OrderPageDataDto;
import com.example.stock.dto.StockCandleWithPrevCloseDto;
import com.example.stock.model.Tickers;
import com.example.stock.model.UserWallet;
import com.example.stock.model.Users;
import com.example.stock.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderPageDataService {

	private final SecurityUtils securityUtils;
	private final TickersService tickersService;
	private final UserWalletService userWalletService;
	private final UserStockService userStockService;
	private final StockService stockService;

	/**
	 * 注文ページに必要な情報を取得し、DTOでまとめて返す。
	 * 取得に失敗した場合は null を返す。
	 */
	public OrderPageDataDto getOrderPageData(String symbol) {
		if (symbol == null || symbol.trim().isEmpty()) {
			return null;
		}

		Users user = securityUtils.getLoggedInUserOrThrow();
		if (user == null)
			return null;

		Tickers ticker = tickersService.getTickersBySymbol(symbol);
		if (ticker == null)
			return null;

		StockCandleWithPrevCloseDto stock = stockService.getLatestStockWithPrevClose(symbol);
		if (stock == null)
			return null;

		UserWallet wallet = userWalletService.getWalletByUser(user);
		if (wallet == null)
			return null;

		BigDecimal quantity = userStockService.getStockQuantityByUserAndTicker(user, symbol);
		if (quantity == null)
			quantity = BigDecimal.ZERO;

		return new OrderPageDataDto(
				user,
				ticker,
				stock,
				wallet.getJpyBalance(),
				wallet.getUsdBalance(),
				quantity);
	}
}
