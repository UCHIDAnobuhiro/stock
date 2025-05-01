package com.example.stock.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.example.stock.converter.TradeConverter;
import com.example.stock.dto.OrderPageDataDto;
import com.example.stock.dto.StockCandleWithPrevCloseDto;
import com.example.stock.dto.TradeRequestDto;
import com.example.stock.model.Tickers;
import com.example.stock.model.Trade;
import com.example.stock.model.Users;
import com.example.stock.security.SecurityUtils;
import com.example.stock.service.LogoDetectionService;
import com.example.stock.service.OrderPageDataService;
import com.example.stock.service.TickersService;
import com.example.stock.service.TradeService;
import com.example.stock.service.UserStockService;
import com.example.stock.service.UserWalletService;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class OrderControllerTest {

	@MockBean
	private LogoDetectionService mockLogoDetectionService;

	@MockBean
	private TradeService tradeService;
	@MockBean
	private TickersService tickersService;
	@MockBean
	private TradeConverter tradeConverter;
	@MockBean
	private UserWalletService userWalletService;
	@MockBean
	private UserStockService userStockService;
	@MockBean
	private PasswordEncoder passwordEncoder;
	@MockBean
	private SecurityUtils securityUtils;

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private OrderPageDataService orderPageDataService;

	private TradeRequestDto testRequestDto;
	private Tickers testTicker;
	private Users testUser;

	@BeforeEach
	void setup() {
		testUser = new Users();
		testUser.setUsername("テスト太郎");
		testUser.setPassword("hashed-password");

		testTicker = new Tickers();
		testTicker.setBrand("Apple");
		testTicker.setTicker("AAPL");
		testTicker.setId(1L);

		testRequestDto = new TradeRequestDto(
				testTicker.getId(),
				new BigDecimal("10"),
				new BigDecimal("150"),
				"JPY",
				new BigDecimal("1"),
				"buy",
				"LIMIT",
				testUser.getPassword());

	}

	@DisplayName("T-301: OrderPageDataDtoがnullまたは不完全な場合、「stock」ページを返す")
	@Test
	void testShowOrderPage_serviceReturnsNull_returnsStockView() throws Exception {
		StockCandleWithPrevCloseDto stock = new StockCandleWithPrevCloseDto(
				"AAPL", "1day", "2025-04-09 00:00:00",
				170.00, 175.00, 169.50, 172.42, 184067400L, 171.95);

		OrderPageDataDto dto = new OrderPageDataDto(null, testTicker, stock, null, null, null);
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
		OrderPageDataDto dto = new OrderPageDataDto(
				testUser,
				testTicker,
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

	@DisplayName("T-303: 入力エラー時はorderページに戻る")
	@Test
	void testShowOrderCheckPage_inputError_returnsOrderView() throws Exception {
		OrderPageDataDto dto = new OrderPageDataDto(
				testUser,
				testTicker,
				new StockCandleWithPrevCloseDto(),
				new BigDecimal("1000"),
				new BigDecimal("500"),
				new BigDecimal("20"));

		//入力エラー値を設定
		testRequestDto.setUnitPrice(new BigDecimal("-1"));

		when(orderPageDataService.getOrderPageData("AAPL")).thenReturn(dto);
		when(tickersService.getTickerById(testTicker.getId())).thenReturn(testTicker);
		when(securityUtils.getLoggedInUserOrThrow()).thenReturn(testUser);

		mockMvc.perform(post("/stock/order/submit")
				.flashAttr("tradeRequestDto", testRequestDto))
				.andExpect(status().isOk())
				.andExpect(view().name("order"))
				.andExpect(model().attributeExists("errorMessage"));
	}

	@DisplayName("T-304: パスワード認証エラー時はorderページに戻る")
	@Test
	void testShowOrderCheckPage_invalidPassword_returnsOrderView() throws Exception {
		testRequestDto.setTradingPin("wrongpin");

		when(tickersService.getTickerById(1L)).thenReturn(testTicker);
		when(securityUtils.getLoggedInUserOrThrow()).thenReturn(testUser);
		when(passwordEncoder.matches(testRequestDto.getTradingPin(), testUser.getPassword())).thenReturn(false);
		when(orderPageDataService.getOrderPageData("AAPL")).thenReturn(new OrderPageDataDto(testUser, testTicker,
				new StockCandleWithPrevCloseDto(), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN));

		mockMvc.perform(post("/stock/order/submit")
				.flashAttr("tradeRequestDto", testRequestDto))
				.andExpect(status().isOk())
				.andExpect(view().name("order"))
				.andExpect(model().attribute("errorMessage", containsString("パスワード")));
	}

	//もし業務バリデーション（tradeService.validateTrade）がなんのエラーが発生する場合、orderページに戻る
	@DisplayName("T-305: 業務バリデーションエラー時はorderページに戻る")
	@Test
	void testShowOrderCheckPage_validationFails_returnsOrderView() throws Exception {

		Trade trade = new Trade();

		when(tickersService.getTickerById(1L)).thenReturn(testTicker);
		when(securityUtils.getLoggedInUserOrThrow()).thenReturn(testUser);
		when(passwordEncoder.matches(testRequestDto.getTradingPin(), testUser.getPassword())).thenReturn(true);
		when(orderPageDataService.getOrderPageData("AAPL")).thenReturn(new OrderPageDataDto(testUser, testTicker,
				new StockCandleWithPrevCloseDto(), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN));
		when(tradeConverter.toTradeEntity(testRequestDto)).thenReturn(trade);
		doThrow(new IllegalStateException("残高不足")).when(tradeService).validateTrade(any());

		mockMvc.perform(post("/stock/order/submit")
				.flashAttr("tradeRequestDto", testRequestDto))
				.andExpect(status().isOk())
				.andExpect(view().name("order"))
				.andExpect(model().attribute("errorMessage", containsString("残高不足")));
	}

	@DisplayName("T-306: 正常処理時はorder-checkページへ遷移")
	@Test
	void testShowOrderCheckPage_success_returnsCheckView() throws Exception {

		Trade trade = new Trade();

		OrderPageDataDto updatedData = new OrderPageDataDto(testUser, testTicker, new StockCandleWithPrevCloseDto(),
				BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);

		when(tickersService.getTickerById(1L)).thenReturn(testTicker);
		when(securityUtils.getLoggedInUserOrThrow()).thenReturn(testUser);
		when(passwordEncoder.matches(testRequestDto.getTradingPin(), testUser.getPassword())).thenReturn(true);
		when(tradeConverter.toTradeEntity(testRequestDto)).thenReturn(trade);
		when(orderPageDataService.getOrderPageData("AAPL")).thenReturn(updatedData);

		mockMvc.perform(post("/stock/order/submit")
				.flashAttr("tradeRequestDto", testRequestDto))
				.andExpect(status().isOk())
				.andExpect(view().name("order-check"))
				.andExpect(model().attributeExists("data"));

		verify(tradeService).executeTrade(trade);
	}

}
