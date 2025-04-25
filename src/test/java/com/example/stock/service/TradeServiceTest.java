package com.example.stock.service;

import static org.assertj.core.api.Assertions.*;
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

	@MockBean
	private StockService stockService;
	@MockBean
	private LogoDetectionService mockLogoDetectionService;

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

	@DisplayName("T-401: 負の数量は保存時にバリデーションエラー（異常系）")
	@Test
	void test401_negativeQuantity_shouldFailValidation() {
		Trade trade = createTrade(new BigDecimal("-5"), new BigDecimal("100"), "JPY");
		assertThatThrownBy(() -> tradeService.saveTrade(trade))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@DisplayName("T-402: 単価が負数は保存時にバリデーションエラー（異常系）")
	@Test
	void test402_negativeUnitPrice_shouldFailValidation() {
		Trade trade = createTrade(new BigDecimal("1"), new BigDecimal("-100.50"), "USD");
		assertThatThrownBy(() -> tradeService.saveTrade(trade))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@DisplayName("T-403: 単価が0は保存時にバリデーションエラー（異常系）")
	@Test
	void test403_zeroUnitPrice_shouldFailValidation() {
		Trade trade = createTrade(new BigDecimal("1"), BigDecimal.ZERO, "USD");
		assertThatThrownBy(() -> tradeService.saveTrade(trade))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@DisplayName("T-404: 極端な大きな値は正常に保存できる（正常系）")
	@Test
	void test404_extremeValues_shouldPass() {
		Trade trade = createTrade(new BigDecimal("999999999"), new BigDecimal("999999.99"), "USD");
		Trade saved = tradeService.saveTrade(trade);
		assertThat(saved).isNotNull();
		assertThat(saved.getId()).isNotNull();
	}

	@DisplayName("T-405: validateTrade 成功（正常系）")
	@Test
	void test405_validateTrade_success() {
		Trade trade = createTrade(new BigDecimal("1"), new BigDecimal("100"), "USD"); // close = 100
		assertThatCode(() -> tradeService.validateTrade(trade)).doesNotThrowAnyException();
	}

	@DisplayName("T-406: validateTrade - 残高不足で例外（異常系）")
	@Test
	void test406_validateTrade_insufficientBalance() {
		Trade trade = createTrade(new BigDecimal("2"), new BigDecimal("1000"), "USD"); // total = 200 > wallet USD 200
		assertThatThrownBy(() -> tradeService.validateTrade(trade))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("残高不足");
	}

	@DisplayName("T-407: validateTrade - 値幅制限違反で例外（異常系）")
	@Test
	void test407_validateTrade_priceOutOfLimit() {
		Trade trade = createTrade(new BigDecimal("1"), new BigDecimal("130"), "USD"); // close = 100, 上限110
		assertThatThrownBy(() -> tradeService.validateTrade(trade))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("値幅制限");
	}

}
