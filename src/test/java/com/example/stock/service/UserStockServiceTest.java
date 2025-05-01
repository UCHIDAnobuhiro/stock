package com.example.stock.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.stock.dto.StockCandleWithPrevCloseDto;
import com.example.stock.model.Tickers;
import com.example.stock.model.Trade;
import com.example.stock.model.UserStock;
import com.example.stock.model.UserWallet;
import com.example.stock.model.Users;
import com.example.stock.repository.TickersRepository;
import com.example.stock.repository.TradeRepository;
import com.example.stock.repository.UserStockRepository;
import com.example.stock.repository.UserWalletLogRepository;
import com.example.stock.repository.UserWalletRepository;
import com.example.stock.repository.UsersRepository;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class UserStockServiceTest {

	@MockBean
	private StockService stockService;

	@MockBean
	private LogoDetectionService mockLogoDetectionService;

	@Autowired
	private UserStockService userStockService;

	@Autowired
	private UserStockRepository userStockRepository;

	@Autowired
	private UsersRepository usersRepository;

	@Autowired
	private TickersRepository tickersRepository;

	@Autowired
	private UserWalletService userWalletService;

	@Autowired
	private TradeRepository tradeRepository;

	@Autowired
	private UserWalletLogRepository userWalletLogRepository;

	@Autowired
	private UserWalletRepository userWalletRepository;

	private Users testUser;
	private Users testUser2;
	private Tickers testTicker;
	private Tickers testTicker2;
	private UserWallet testWallet;

	@BeforeEach
	void setup() {
		userWalletLogRepository.deleteAll(); // 最子级：引用了 trade 和 wallet
		tradeRepository.deleteAll(); // 引用了 ticker 和 user
		userWalletRepository.deleteAll(); // 引用了 user
		tickersRepository.deleteAll(); // 引用了无
		usersRepository.deleteAll(); // 父表

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

		testUser2 = new Users();
		testUser2.setUsername("株式太郎");
		testUser2.setEmail("stockuser2@example.com");
		testUser2.setPassword("$2a$10$hBrJiyk7dArR3hGR7bvu5.oYKlK6O506lRvqdl8WTIvu1bxV22EJy");
		testUser2.setCreateAt(LocalDateTime.now());
		testUser2.setUpdateAt(LocalDateTime.now());
		testUser2.setEnabled(true);
		testUser2.setFailedLoginAttempts(0);
		testUser2.setAccountLocked(false);
		testUser2.setLockTime(null);
		usersRepository.save(testUser2);

		testTicker = new Tickers();
		testTicker.setTicker("AAPL");
		testTicker.setBrand("Apple Inc.");
		tickersRepository.save(testTicker);

		testTicker2 = new Tickers();
		testTicker2.setTicker("GOOG");
		testTicker2.setBrand("Google LLC");
		tickersRepository.save(testTicker2);

		testWallet = userWalletService.getWalletByUser(testUser);
		testWallet.setJpyBalance(new BigDecimal("5000"));
		testWallet.setUsdBalance(new BigDecimal("100"));
		userWalletRepository.save(testWallet);

		StockCandleWithPrevCloseDto mockDto = new StockCandleWithPrevCloseDto();
		mockDto.setClose(100.0);
		mockDto.setPrevClose(100.0);
		mockDto.setSymbol("AAPL");
		mockDto.setInterval("1day");
		when(stockService.getLatestStockWithPrevClose("AAPL")).thenReturn(mockDto);
	}

	private UserStock buildStock(Users user, Tickers ticker, BigDecimal quantity) {
		UserStock stock = new UserStock();
		stock.setUser(user);
		stock.setTicker(ticker);
		stock.setQuantity(quantity);
		stock.setCreateAt(LocalDateTime.now());
		stock.setUpdateAt(LocalDateTime.now());
		return stock;
	}

	@DisplayName("T-101: ユーザーが株式を保有していない場合、数量0が返る")
	@Test
	void testGetStockQuantity_returnsZeroIfNoStock() {
		BigDecimal quantity = userStockService.getStockQuantityByUserAndTicker(testUser, testTicker.getTicker());
		assertThat(quantity).isEqualTo(BigDecimal.ZERO);
	}

	@DisplayName("T-102: ユーザーが株式を保有している場合、正しい数量が返る")
	@Test
	void testGetStockQuantity_returnsCorrectQuantity() {
		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("42.75")));
		BigDecimal quantity = userStockService.getStockQuantityByUserAndTicker(testUser, testTicker.getTicker());
		assertThat(quantity).isEqualTo(new BigDecimal("42.75"));
	}

	@DisplayName("T-103: 存在しない ticker シンボルを指定した場合、数量0が返る")
	@Test
	void testGetStockQuantity_withInvalidSymbol_shouldThrowOrReturnZero() {
		BigDecimal quantity = userStockService.getStockQuantityByUserAndTicker(testUser, "INVALID");
		assertThat(quantity).isEqualTo(BigDecimal.ZERO);
	}

	@DisplayName("T-104: 最大数量（9999999999999999.99）が正しく処理できる")
	@Test
	void testGetStockQuantity_withMaxValue() {
		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("9999999999999999.99")));
		BigDecimal quantity = userStockService.getStockQuantityByUserAndTicker(testUser, testTicker.getTicker());
		assertThat(quantity).isEqualTo(new BigDecimal("9999999999999999.99"));
	}

	@DisplayName("T-105: 最小有効数量（0.01）を正しく処理できる")
	@Test
	void testGetStockQuantity_withMinPositiveValue() {
		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("0.01")));
		BigDecimal quantity = userStockService.getStockQuantityByUserAndTicker(testUser, testTicker.getTicker());
		assertThat(quantity).isEqualTo(new BigDecimal("0.01"));
	}

	@DisplayName("T-106: 負の数量も処理できる（ルール上許可）")
	@Test
	void testGetStockQuantity_withNegativeQuantity() {
		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("-50")));
		BigDecimal quantity = userStockService.getStockQuantityByUserAndTicker(testUser, testTicker.getTicker());
		assertThat(quantity).isEqualTo(new BigDecimal("-50"));
	}

	@DisplayName("T-107: 複数ユーザーが同じ銘柄を保有していても正しく分離される")
	@Test
	void testGetStockQuantity_multiUserSeparation() {
		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("100")));
		userStockRepository.save(buildStock(testUser2, testTicker, new BigDecimal("200")));

		BigDecimal aQuantity = userStockService.getStockQuantityByUserAndTicker(testUser, testTicker.getTicker());
		BigDecimal bQuantity = userStockService.getStockQuantityByUserAndTicker(testUser2, testTicker.getTicker());

		assertThat(aQuantity).isEqualTo(new BigDecimal("100"));
		assertThat(bQuantity).isEqualTo(new BigDecimal("200"));
	}

	@DisplayName("T-108: ユーザーが複数銘柄を保有していても個別に数量を取得できる")
	@Test
	void testGetStockQuantity_multiTickerSeparation() {
		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("10"))); // AAPL
		userStockRepository.save(buildStock(testUser, testTicker2, new BigDecimal("5"))); // GOOG

		BigDecimal quantityAAPL = userStockService.getStockQuantityByUserAndTicker(testUser, testTicker.getTicker());
		BigDecimal quantityGOOG = userStockService.getStockQuantityByUserAndTicker(testUser, testTicker2.getTicker());

		assertThat(quantityAAPL).isEqualTo(new BigDecimal("10"));
		assertThat(quantityGOOG).isEqualTo(new BigDecimal("5"));
	}

	@DisplayName("T-109: DECIMAL(18,2) を超える数量は保存されない")
	@Test
	void testGetStockQuantity_overMaxDecimal_shouldFail() {
		BigDecimal overLimit = new BigDecimal("100000000000000000.00");
		UserStock stock = buildStock(testUser, testTicker, overLimit);

		try {
			userStockRepository.save(stock);
			fail("例外が発生しないのは想定外です");
		} catch (Exception e) {
			assertThat(e).isInstanceOf(Exception.class);
		}
	}

	@DisplayName("T-110: 銘柄が null の場合は例外が保存されない")
	@Test
	void testGetStockQuantity_nullFields_shouldFail() {
		UserStock invalidStock = buildStock(testUser, null, new BigDecimal("10"));

		try {
			userStockRepository.save(invalidStock);
			fail("nullユーザーで例外が出ないのはバグです");
		} catch (Exception e) {
			assertThat(e).isInstanceOf(Exception.class);
		}
	}

	@Test
	@DisplayName("T-113: 正常な買い注文でUserStockが作成・数量加算される")
	void testApplyTradeToUserStock_buy_createsAndAdds() {
		// 保有なし → 新規作成される
		Trade trade = createTrade(testUser, testTicker, "JPY", new BigDecimal("100"), 0);
		trade.setQuantity(new BigDecimal("3"));

		userStockService.applyTradeToUserStock(trade);

		UserStock result = userStockRepository.findByUserAndTicker(testUser, testTicker).orElse(null);
		assertThat(result).isNotNull();
		assertThat(result.getQuantity()).isEqualTo(new BigDecimal("3"));
	}

	@Test
	@DisplayName("T-114: 売り注文で保有株数が不足していると例外を投げる")
	void testApplyTradeToUserStock_sell_insufficientStock() {
		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("1")));

		Trade trade = createTrade(testUser, testTicker, "JPY", new BigDecimal("100"), 1);
		trade.setQuantity(new BigDecimal("2")); // 保有数 < 売却数

		assertThatThrownBy(() -> userStockService.applyTradeToUserStock(trade))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("保有株数が不足");
	}

	@Test
	@DisplayName("T-115: 売り注文で保有数が十分な場合は数量が正しく減る")
	void testApplyTradeToUserStock_sell_successfulReduction() {
		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("5")));

		Trade trade = createTrade(testUser, testTicker, "JPY", new BigDecimal("100"), 1);
		trade.setQuantity(new BigDecimal("2"));

		userStockService.applyTradeToUserStock(trade);

		UserStock updated = userStockRepository.findByUserAndTicker(testUser, testTicker).orElseThrow();
		assertThat(updated.getQuantity()).isEqualTo(new BigDecimal("3"));
	}

	@Test
	@DisplayName("T-116: 売り注文で保有数と売却数が同じ場合、数量が0になる")
	void testApplyTradeToUserStock_sell_reducesToZero() {
		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("3")));

		Trade trade = createTrade(testUser, testTicker, "JPY", new BigDecimal("300"), 1);
		trade.setQuantity(new BigDecimal("3"));

		userStockService.applyTradeToUserStock(trade);

		UserStock updated = userStockRepository.findByUserAndTicker(testUser, testTicker).orElseThrow();
		assertThat(updated.getQuantity()).isEqualTo(BigDecimal.ZERO);
	}

	private Trade createTrade(Users user, Tickers ticker, String currency, BigDecimal total, int side) {
		Trade t = new Trade();
		t.setUser(user);
		t.setTicker(ticker);
		t.setSettlementCurrency(currency);
		t.setCurrency(currency);
		t.setSide(side);
		t.setTotalPrice(total);
		t.setUnitPrice(total);
		t.setExchangeRate(BigDecimal.ONE);
		t.setQuantity(BigDecimal.ONE);
		t.setCreateAt(LocalDateTime.now());
		t.setUpdateAt(LocalDateTime.now());
		return tradeRepository.save(t);
	}
}
