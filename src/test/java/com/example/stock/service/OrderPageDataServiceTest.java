package com.example.stock.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.stock.dto.OrderPageDataDto;
import com.example.stock.dto.StockCandleWithPrevCloseDto;
import com.example.stock.model.Tickers;
import com.example.stock.model.UserWallet;
import com.example.stock.model.Users;
import com.example.stock.security.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class OrderPageDataServiceTest {

	@InjectMocks
	private OrderPageDataService orderPageDataService;

	@Mock
	private SecurityUtils securityUtils;

	@Mock
	private TickersService tickersService;

	@Mock
	private UserWalletService userWalletService;

	@Mock
	private UserStockService userStockService;

	@Mock
	private StockService stockService;

	private Users user;
	private Tickers ticker;
	private UserWallet wallet;
	private StockCandleWithPrevCloseDto stock;

	@BeforeEach
	void setup() {
		user = new Users();
		user.setId(1L);
		user.setUsername("テスト太郎");

		ticker = new Tickers();
		ticker.setTicker("AAPL");
		ticker.setBrand("Apple");

		wallet = new UserWallet();
		wallet.setUser(user);
		wallet.setJpyBalance(new BigDecimal("1000"));
		wallet.setUsdBalance(new BigDecimal("100"));

		stock = new StockCandleWithPrevCloseDto(); // 内容は簡略化
	}

	/**
	 * 正常に全ての情報が取得できた場合、DTO が正しく返されるかを検証
	 */
	@Test
	void testGetOrderPageData_allValid_returnsDto() {
		when(securityUtils.getLoggedInUserOrThrow()).thenReturn(user);
		when(tickersService.getTickersBySymbol("AAPL")).thenReturn(ticker);
		when(stockService.getLatestStockWithPrevClose("AAPL")).thenReturn(stock);
		when(userWalletService.getWalletByUser(user)).thenReturn(wallet);
		when(userStockService.getStockQuantityByUserAndTicker(user, "AAPL"))
				.thenReturn(new BigDecimal("50.25"));

		OrderPageDataDto result = orderPageDataService.getOrderPageData("AAPL");

		assertThat(result).isNotNull();
		assertThat(result.getUser()).isEqualTo(user);
		assertThat(result.getTicker()).isEqualTo(ticker);
		assertThat(result.getStock()).isEqualTo(stock);
		assertThat(result.getJpyBalance()).isEqualTo(new BigDecimal("1000"));
		assertThat(result.getUsdBalance()).isEqualTo(new BigDecimal("100"));
		assertThat(result.getQuantity()).isEqualTo(new BigDecimal("50.25"));
	}

	/**
	 * symbol が空文字のとき、null を返す
	 */
	@Test
	void testGetOrderPageData_symbolEmpty_returnsNull() {
		OrderPageDataDto result = orderPageDataService.getOrderPageData(" ");
		assertNull(result);
	}

	/**
	 * Ticker が見つからなかった場合、null を返す
	 */
	@Test
	void testGetOrderPageData_tickerNotFound_returnsNull() {
		when(securityUtils.getLoggedInUserOrThrow()).thenReturn(user);
		when(tickersService.getTickersBySymbol("AAPL")).thenReturn(null);

		OrderPageDataDto result = orderPageDataService.getOrderPageData("AAPL");
		assertNull(result);
	}

	/**
	 * Stock 情報が取得できなかった場合、null を返す
	 */
	@Test
	void testGetOrderPageData_stockNotFound_returnsNull() {
		when(securityUtils.getLoggedInUserOrThrow()).thenReturn(user);
		when(tickersService.getTickersBySymbol("AAPL")).thenReturn(ticker);
		when(stockService.getLatestStockWithPrevClose("AAPL")).thenReturn(null);

		OrderPageDataDto result = orderPageDataService.getOrderPageData("AAPL");
		assertNull(result);
	}

	/**
	 * Wallet が取得できなかった場合、null を返す
	 */
	@Test
	void testGetOrderPageData_walletNotFound_returnsNull() {
		when(securityUtils.getLoggedInUserOrThrow()).thenReturn(user);
		when(tickersService.getTickersBySymbol("AAPL")).thenReturn(ticker);
		when(stockService.getLatestStockWithPrevClose("AAPL")).thenReturn(stock);
		when(userWalletService.getWalletByUser(user)).thenReturn(null);

		OrderPageDataDto result = orderPageDataService.getOrderPageData("AAPL");
		assertNull(result);
	}

	/**
	 * quantity が null の場合、0 に変換されて返される
	 */
	@Test
	void testGetOrderPageData_quantityNull_shouldReturnZero() {
		when(securityUtils.getLoggedInUserOrThrow()).thenReturn(user);
		when(tickersService.getTickersBySymbol("AAPL")).thenReturn(ticker);
		when(stockService.getLatestStockWithPrevClose("AAPL")).thenReturn(stock);
		when(userWalletService.getWalletByUser(user)).thenReturn(wallet);
		when(userStockService.getStockQuantityByUserAndTicker(user, "AAPL")).thenReturn(null);

		OrderPageDataDto result = orderPageDataService.getOrderPageData("AAPL");

		assertThat(result).isNotNull();
		assertThat(result.getQuantity()).isEqualTo(BigDecimal.ZERO);
	}
}
