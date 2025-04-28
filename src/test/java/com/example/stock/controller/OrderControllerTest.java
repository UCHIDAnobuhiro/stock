package com.example.stock.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
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

	@DisplayName("T-301: OrderPageDataDtoがnullまたは不完全な場合、「stock」ページを返す")
	@Test
	void testShowOrderPage_serviceReturnsNull_returnsStockView() throws Exception {
		Tickers ticker = new Tickers();
		ticker.setBrand("Apple");
		ticker.setTicker("AAPL");

		StockCandleWithPrevCloseDto stock = new StockCandleWithPrevCloseDto(
				"AAPL", "1day", "2025-04-09 00:00:00",
				170.00, 175.00, 169.50, 172.42, 184067400L, 171.95);

		OrderPageDataDto dto = new OrderPageDataDto(null, ticker, stock, null, null, null);
		when(orderPageDataService.getOrderPageData("AAPL")).thenReturn(dto);

		mockMvc.perform(MockMvcRequestBuilders.get("/stock/order")
				.with(user("testuser").roles("USER"))
				.param("orderType", "buy")
				.param("symbol", "AAPL"))
				.andExpect(status().isOk())
				.andExpect(view().name("stock"))
				.andExpect(model().attributeExists("ticker"))
				.andExpect(model().attributeExists("stock"));
	}

	@DisplayName("T-302: 正常なOrderPageDataDtoが返る場合、「order」ページを返す")
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
				.andExpect(model().attributeExists("data"))
				.andExpect(model().attribute("data", hasProperty("user", hasProperty("displayName", is("テスト太郎")))))
				.andExpect(model().attribute("data", hasProperty("ticker", hasProperty("ticker", is("AAPL")))))
				.andExpect(model().attribute("data", hasProperty("jpyBalance", is(new BigDecimal("1000")))))
				.andExpect(model().attribute("data", hasProperty("usdBalance", is(new BigDecimal("500")))))
				.andExpect(model().attribute("data", hasProperty("quantity", is(new BigDecimal("20")))))
				.andExpect(model().attribute("orderType", is("buy")));
	}
}
