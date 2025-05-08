package com.example.stock.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.stock.model.Tickers;
import com.example.stock.model.Trade;
import com.example.stock.model.Users;
import com.example.stock.security.SecurityUtils;
import com.example.stock.service.LogoDetectionService;
import com.example.stock.service.TradeService;

@WebMvcTest(TradeController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class TradeControllerTest {

	@MockBean
	private LogoDetectionService mockLogoDetectionService;
	@MockBean
	private TradeService tradeService;
	@MockBean
	private SecurityUtils securityUtils;

	@Autowired
	private MockMvc mockMvc;

	private Users mockUser() {
		Users user = new Users();
		user.setUsername("テスト太郎");
		user.setPassword("hashed-password");
		return user;
	}

	private Trade mockTrade(String tickerCode) {
		Trade trade = new Trade();
		trade.setId(1L);
		trade.setUser(mockUser());
		Tickers ticker = new Tickers();
		ticker.setTicker(tickerCode);
		trade.setTicker(ticker);
		return trade;
	}

	@DisplayName("T601: /trade-log に正常アクセスできること")
	@Test
	void showTradeLog_shouldReturnViewWithTrades() throws Exception {
		Users user = mockUser();
		when(securityUtils.getLoggedInUserOrThrow()).thenReturn(user);
		when(tradeService.getTradesByUser(user)).thenReturn(List.of(mockTrade("AAPL")));

		mockMvc.perform(get("/trade-log"))
				.andExpect(status().isOk())
				.andExpect(view().name("trade-log"))
				.andExpect(model().attributeExists("trades"));
	}

	@DisplayName("T602: 英字ティッカーで検索成功すること")
	@Test
	void searchTrades_validTicker_shouldReturnTrades() throws Exception {
		Users user = mockUser();
		when(securityUtils.getLoggedInUserOrThrow()).thenReturn(user);
		when(tradeService.searchTrades(user, "all", "AAPL")).thenReturn(List.of(mockTrade("AAPL")));

		mockMvc.perform(get("/trade-log/search").param("date", "all").param("ticker", "AAPL"))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("trades"))
				.andExpect(view().name("fragments/order/trade-log-show :: trade-table-fragment"))
				.andExpect(model().attributeDoesNotExist("errorMessage"));
	}

	@DisplayName("T603: 該当なし検索時にエラーメッセージを表示すること")
	@Test
	void searchTrades_noResult_shouldShowNoResultMessage() throws Exception {
		Users user = mockUser();
		when(securityUtils.getLoggedInUserOrThrow()).thenReturn(user);
		when(tradeService.searchTrades(user, "all", "NOTFOUND")).thenReturn(Collections.emptyList());

		mockMvc.perform(get("/trade-log/search").param("date", "all").param("ticker", "XYZ"))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("errorMessage"))
				.andExpect(view().name("fragments/order/trade-log-show :: trade-table-fragment"));
	}

	@DisplayName("T604: ティッカーが英字以外の場合はバリデーションエラーが表示されること")
	@Test
	void searchTrades_invalidTicker_shouldShowValidationError() throws Exception {
		Users user = mockUser();
		when(securityUtils.getLoggedInUserOrThrow()).thenReturn(user);

		mockMvc.perform(get("/trade-log/search").param("ticker", "あAPL"))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("errorMessage"))
				.andExpect(view().name("fragments/order/trade-log-show :: trade-table-fragment"));
	}

	@DisplayName("T605: ティッカーが空文字でも全件検索が行われること")
	@Test
	void searchTrades_emptyTicker_shouldReturnAll() throws Exception {
		Users user = mockUser();
		when(securityUtils.getLoggedInUserOrThrow()).thenReturn(user);
		when(tradeService.searchTrades(user, "all", "")).thenReturn(List.of(mockTrade("AAPL")));

		mockMvc.perform(get("/trade-log/search").param("ticker", ""))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("trades"))
				.andExpect(view().name("fragments/order/trade-log-show :: trade-table-fragment"));
	}

}
