package com.example.stock.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.stock.dto.StockCandleWithPrevCloseDto;
import com.example.stock.model.Tickers;
import com.example.stock.model.Trade;
import com.example.stock.model.UserWallet;
import com.example.stock.model.Users;
import com.example.stock.repository.TickersRepository;
import com.example.stock.repository.TradeRepository;
import com.example.stock.repository.UserWalletRepository;
import com.example.stock.repository.UsersRepository;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class TradeServiceTest {

	@Autowired
	private TradeService tradeService;
	@Autowired
	private UsersRepository usersRepository;
	@Autowired
	private TickersRepository tickersRepository;
	@Autowired
	private UserWalletRepository walletRepository;
	@Autowired
	private TradeRepository tradeRepository;

	@MockBean
	private StockService stockService;
	@MockBean
	private LogoDetectionService mockLogoDetectionService;
	@MockBean
	private UserStockService userStockService;
	@MockBean
	private UserWalletService userWalletService;

	private Users testUser;
	private Tickers testTicker;
	private UserWallet testWallet;

	@BeforeEach
	void setup() {
		testUser = new Users();
		testUser.setUsername("株式太郎");
		testUser.setEmail("stockuser@example.com");
		testUser.setPassword("$2a$10$hBrJiyk7dArR3hGR7bvu5.oYKlK6O506lRvqdl8WTIvu1bxV22EJy");
		testUser.setCreateAt(LocalDateTime.now());
		testUser.setUpdateAt(LocalDateTime.now());
		testUser.setEnabled(true);
		testUser.setFailedLoginAttempts(0);
		testUser.setAccountLocked(false);
		testUser.setLockTime(null);
		usersRepository.save(testUser);

		testTicker = new Tickers();
		testTicker.setTicker("AAPL");
		testTicker.setBrand("Apple Inc.");
		tickersRepository.save(testTicker);

		testWallet = new UserWallet();
		testWallet.setUser(testUser);
		testWallet.setJpyBalance(new BigDecimal("5000"));
		testWallet.setUsdBalance(new BigDecimal("200"));
		testWallet.setCreateAt(LocalDateTime.now());
		testWallet.setUpdateAt(LocalDateTime.now());
		walletRepository.save(testWallet);

		StockCandleWithPrevCloseDto mockDto = new StockCandleWithPrevCloseDto();
		mockDto.setClose(100.0);
		mockDto.setPrevClose(100.0);
		mockDto.setSymbol("AAPL");
		mockDto.setInterval("1day");
		when(stockService.getLatestStockWithPrevClose("AAPL")).thenReturn(mockDto);
		when(userWalletService.getWalletByUser(eq(testUser))).thenReturn(testWallet);
	}

	private Trade createTrade(BigDecimal qty, BigDecimal unitPrice, String currency) {
		Trade t = new Trade();
		t.setUser(testUser);
		t.setTicker(testTicker);
		t.setQuantity(qty);
		t.setUnitPrice(unitPrice);
		t.setTotalPrice(qty.multiply(unitPrice));
		t.setCurrency(currency);
		t.setSettlementCurrency(currency);
		t.setExchangeRate(BigDecimal.ONE);
		t.setSide(0); // buy
		t.setType(0); // limit
		t.setStatus(4);
		t.setCreateAt(LocalDateTime.now());
		t.setUpdateAt(LocalDateTime.now());
		return t;
	}

	@DisplayName("T-401: 負の数量はバリデーションエラー（異常系）")
	@Test
	void negativeQuantity_shouldFailValidation() {
		Trade trade = createTrade(new BigDecimal("-5"), new BigDecimal("100"), "JPY");
		assertThatThrownBy(() -> tradeService.executeTrade(trade))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@DisplayName("T-402: 単価が負数はバリデーションエラー（異常系）")
	@Test
	void negativeUnitPrice_shouldFailValidation() {
		Trade trade = createTrade(new BigDecimal("1"), new BigDecimal("-100.50"), "USD");
		assertThatThrownBy(() -> tradeService.executeTrade(trade))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@DisplayName("T-403: 単価が0はバリデーションエラー（異常系）")
	@Test
	void zeroUnitPrice_shouldFailValidation() {
		Trade trade = createTrade(new BigDecimal("1"), BigDecimal.ZERO, "USD");
		assertThatThrownBy(() -> tradeService.executeTrade(trade))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@DisplayName("T-404: 極端な大きな値でも正常に取引実行できる（正常系）")
	@Test
	void extremeValues_shouldPass() {
		testWallet.setUsdBalance(new BigDecimal("9999999999999999"));
		walletRepository.save(testWallet);

		StockCandleWithPrevCloseDto mockHighDto = new StockCandleWithPrevCloseDto();
		mockHighDto.setClose(999999.99);
		mockHighDto.setPrevClose(999999.99);
		mockHighDto.setSymbol("AAPL");
		mockHighDto.setInterval("1day");
		when(stockService.getLatestStockWithPrevClose("AAPL")).thenReturn(mockHighDto);

		Trade trade = createTrade(new BigDecimal("999999999"), new BigDecimal("999999.99"), "USD");
		assertThatCode(() -> tradeService.executeTrade(trade)).doesNotThrowAnyException();
	}

	@DisplayName("T-405: 注文検証が成功する（正常系）")
	@Test
	void validateTrade_success() {
		Trade trade = createTrade(new BigDecimal("1"), new BigDecimal("100"), "USD");
		assertThatCode(() -> tradeService.validateTrade(trade)).doesNotThrowAnyException();
	}

	@DisplayName("T-406: 注文検証で残高不足の場合は例外が発生する（異常系）")
	@Test
	void validateTrade_insufficientBalance() {
		Trade trade = createTrade(new BigDecimal("2"), new BigDecimal("1000"), "USD");
		assertThatThrownBy(() -> tradeService.validateTrade(trade))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("残高不足");
	}

	@DisplayName("T-407: 注文価格が値幅制限を超えている場合は例外が発生する（異常系）")
	@Test
	void validateTrade_priceOutOfLimit() {
		Trade trade = createTrade(new BigDecimal("1"), new BigDecimal("130"), "USD");
		assertThatThrownBy(() -> tradeService.validateTrade(trade))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("値幅制限");
	}

	@DisplayName("T-408: 買い注文が正常に実行される（全処理成功）")
	@Test
	void executeTrade_success() {
		Trade trade = createTrade(new BigDecimal("1"), new BigDecimal("100"), "USD");
		assertThatCode(() -> tradeService.executeTrade(trade)).doesNotThrowAnyException();
	}

	@DisplayName("T-409: 買い注文で残高不足の場合は例外が発生する（異常系）")
	@Test
	void executeTrade_insufficientBalance() {
		Trade trade = createTrade(new BigDecimal("2"), new BigDecimal("1000"), "USD");
		assertThatThrownBy(() -> tradeService.executeTrade(trade))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("残高不足");
	}

	@DisplayName("T-410: 実行中に例外が発生した場合は以降の処理が中止される（ロールバック確認）")
	@Test
	void executeTrade_abortsAfterException() {
		Trade trade = createTrade(new BigDecimal("1"), new BigDecimal("100"), "USD");

		doThrow(new IllegalStateException("DB更新失敗"))
				.when(userStockService).applyTradeToUserStock(any());

		assertThatThrownBy(() -> tradeService.executeTrade(trade))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("DB更新失敗");

		verify(userWalletService).applyTradeToWallet(any());
		verify(userStockService).applyTradeToUserStock(any());
	}

	@DisplayName("T-411: 売り注文で保有株数が十分な場合は正常に実行される（正常系）")
	@Test
	void executeTrade_sellSuccess() {
		Trade trade = createTrade(new BigDecimal("10"), new BigDecimal("100"), "USD");
		trade.setSide(1); // 売り注文

		when(userStockService.getStockQuantityByUserAndTicker(eq(testUser), eq("AAPL")))
				.thenReturn(new BigDecimal("100"));

		assertThatCode(() -> tradeService.executeTrade(trade)).doesNotThrowAnyException();
	}

	@DisplayName("T-412: 売り注文で保有株数が不足している場合は例外が発生する（異常系）")
	@Test
	void executeTrade_sellInsufficientStock() {
		Trade trade = createTrade(new BigDecimal("100"), new BigDecimal("100"), "USD");
		trade.setSide(1); // 売り注文

		when(userStockService.getStockQuantityByUserAndTicker(eq(testUser), eq("AAPL")))
				.thenReturn(BigDecimal.ZERO);

		assertThatThrownBy(() -> tradeService.executeTrade(trade))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("保有株数が不足");
	}
}
