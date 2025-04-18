package com.example.stock.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.stock.dto.OrderPageDataDto;
import com.example.stock.dto.StockCandleWithPrevCloseDto;
import com.example.stock.model.Tickers;
import com.example.stock.model.Users;
import com.example.stock.service.OrderPageDataService;

@WebMvcTest(OrderController.class)
public class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private OrderPageDataService orderPageDataService;

	/**
	 * symbol が null または空文字の場合、stock ページへリダイレクト
	 */
	@Test
	void testShowOrderPage_withEmptySymbol_returnsStockView() throws Exception {
		mockMvc.perform(get("/stock/order")
				.param("orderType", "BUY")
				.param("symbol", " "))
				.andExpect(status().isOk())
				.andExpect(view().name("stock"));
	}

	/**
	 * OrderPageDataDto が null の場合、stock ページへリダイレクト
	 */
	@Test
	void testShowOrderPage_serviceReturnsNull_returnsStockView() throws Exception {
		when(orderPageDataService.getOrderPageData("AAPL")).thenReturn(null);

		mockMvc.perform(get("/stock/order")
				.param("orderType", "SELL")
				.param("symbol", "AAPL"))
				.andExpect(status().isOk())
				.andExpect(view().name("stock"));
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
				.param("orderType", "BUY")
				.param("symbol", "AAPL"))
				.andExpect(status().isOk())
				.andExpect(view().name("order"))
				.andExpect(model().attributeExists("stock"))
				.andExpect(model().attributeExists("userName"))
				.andExpect(model().attributeExists("jpyBalance"))
				.andExpect(model().attributeExists("usdBalance"))
				.andExpect(model().attributeExists("quantity"))
				.andExpect(model().attributeExists("ticker"))
				.andExpect(model().attribute("orderType", "BUY"));
	}
}
