package com.example.stock.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.example.stock.dto.OrderPageDataDto;
import com.example.stock.dto.StockCandleWithPrevCloseDto;
import com.example.stock.model.Tickers;
import com.example.stock.model.Users;
import com.example.stock.service.OrderPageDataService;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
public class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private OrderPageDataService orderPageDataService;

	/**
	 * OrderPageDataDto が null の場合、stock ページへリダイレクト
	 */
	@Test
	void testShowOrderPage_serviceReturnsNull_returnsStockView() throws Exception {
		Tickers ticker = new Tickers();
		ticker.setBrand("Apple");
		ticker.setTicker("AAPL");

		StockCandleWithPrevCloseDto stock = new StockCandleWithPrevCloseDto(
				"AAPL", // symbol
				"1day", // interval
				"2025-04-09 00:00:00", // datetime
				170.00, // open
				175.00, // high
				169.50, // low
				172.42, // close
				184067400L, // volume
				171.95 // prevClose
		);

		OrderPageDataDto dto = new OrderPageDataDto(null, ticker, stock, null, null, null);
		when(orderPageDataService.getOrderPageData("AAPL")).thenReturn(dto);
		mockMvc.perform(MockMvcRequestBuilders.get("/stock/order")
				.with(user("testuser").roles("USER"))
				.param("orderType", "buy")
				.param("symbol", "AAPL"))
				.andExpect(status().isOk());
	}

	/**
	 * 正常なデータが返る場合、order ページを返す
	 */
	@Test
	void testShowOrderPage_successful_returnsOrderView() throws Exception {
		Users user = new Users();
		user.setUsername("テスト太郎");

		Tickers ticker = new Tickers();
		ticker.setTicker("AAPL");
		ticker.setBrand("Apple Inc.");

		OrderPageDataDto dto = new OrderPageDataDto(
				user,
				ticker,
				new StockCandleWithPrevCloseDto(),
				new BigDecimal("1000"),
				new BigDecimal("500"),
				new BigDecimal("20"));

		when(orderPageDataService.getOrderPageData("AAPL")).thenReturn(dto);

		mockMvc.perform(get("/stock/order")
				.param("orderType", "buy")
				.param("symbol", "AAPL"))
				.andExpect(status().isOk())
				.andExpect(view().name("order"))
				.andExpect(model().attributeExists("stock"))
				.andExpect(model().attributeExists("userName"))
				.andExpect(model().attributeExists("jpyBalance"))
				.andExpect(model().attributeExists("usdBalance"))
				.andExpect(model().attributeExists("quantity"))
				.andExpect(model().attributeExists("ticker"))
				.andExpect(model().attribute("orderType", "buy"));
	}
}
