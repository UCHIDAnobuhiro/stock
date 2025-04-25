package com.example.stock.service;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.stock.model.Tickers;
import com.example.stock.model.Trade;
import com.example.stock.model.UserWallet;
import com.example.stock.model.UserWalletLog;
import com.example.stock.model.Users;
import com.example.stock.repository.TickersRepository;
import com.example.stock.repository.TradeRepository;
import com.example.stock.repository.UserWalletLogRepository;
import com.example.stock.repository.UserWalletRepository;
import com.example.stock.repository.UsersRepository;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class UserWalletLogServiceTest {

	@MockBean
	private LogoDetectionService mockLogoDetectionService;

	@Autowired
	private UserWalletLogService userWalletLogService;

	@Autowired
	private UserWalletLogRepository userWalletLogRepository;

	@Autowired
	private UserWalletRepository userWalletRepository;

	@Autowired
	private TradeRepository tradeRepository;

	@Autowired
	private UsersRepository usersRepository;

	@Autowired
	private TickersRepository tickersRepository;

	@Autowired
	private UserWalletService userWalletService;

	private Users testUser;
	private Users testUser2;
	private Tickers testTicker;
	private UserWallet testWallet;
	private UserWallet testWallet2;
	private UserWalletLog testLog;

	@BeforeEach
	void setup() {
		userWalletLogRepository.deleteAll();
		tradeRepository.deleteAll();
		userWalletRepository.deleteAll();
		usersRepository.deleteAll();
		tickersRepository.deleteAll();

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
		testTicker.setBrand("Apple");
		tickersRepository.save(testTicker);

		testWallet = userWalletService.getWalletByUser(testUser);
		testWallet.setJpyBalance(new BigDecimal("5000"));
		testWallet.setUsdBalance(new BigDecimal("100"));
		userWalletRepository.save(testWallet);

		testWallet2 = userWalletService.getWalletByUser(testUser2);
		testWallet2.setJpyBalance(new BigDecimal("10000"));
		testWallet2.setUsdBalance(new BigDecimal("500"));
		userWalletRepository.save(testWallet2);
	}

	private Trade createTrade(int side, String currency, BigDecimal total) {
		Trade trade = new Trade();
		trade.setUser(testUser);
		trade.setTicker(testTicker);
		trade.setCurrency(currency);
		trade.setSettlementCurrency(currency);
		trade.setSide(side);
		trade.setTotalPrice(total);
		trade.setQuantity(BigDecimal.ONE);
		trade.setUnitPrice(total);
		trade.setExchangeRate(BigDecimal.ONE);
		trade.setCreateAt(LocalDateTime.now());
		trade.setUpdateAt(LocalDateTime.now());
		return tradeRepository.save(trade);
	}

	@Test
	@DisplayName("T-501: JPY買い注文のログが正常に作成される")
	void testCreateLog_JPY_Buy() {
		Trade trade = createTrade(0, "JPY", new BigDecimal("1000"));
		userWalletLogService.createAndSaveLog(trade, testWallet);

		testLog = userWalletLogRepository.findAll().get(0);
		assertThat(testLog.getCurrency()).isEqualTo("JPY");
		assertThat(testLog.getBeforeBalance()).isEqualTo(new BigDecimal("5000"));
		assertThat(testLog.getChangeAmount()).isEqualTo(new BigDecimal("-1000")); // 買い＝減少
		assertThat(testLog.getAfterBalance()).isEqualTo(new BigDecimal("4000"));
	}

	@Test
	@DisplayName("T-502: USD買い注文のログが正常に作成される")
	void testCreateLog_USD_Sell() {
		Trade trade = createTrade(0, "USD", new BigDecimal("10"));
		userWalletLogService.createAndSaveLog(trade, testWallet);

		testLog = userWalletLogRepository.findAll().get(0);
		assertThat(testLog.getCurrency()).isEqualTo("USD");
		assertThat(testLog.getBeforeBalance()).isEqualTo(new BigDecimal("100"));
		assertThat(testLog.getChangeAmount()).isEqualTo(new BigDecimal("-10"));
		assertThat(testLog.getAfterBalance()).isEqualTo(new BigDecimal("90"));
	}

	@Test
	@DisplayName("T-503: 無効な通貨コードで例外が発生する")
	void testCreateLog_invalidCurrency_shouldThrow() {
		Trade trade = createTrade(1, "BTC", new BigDecimal("100"));
		assertThatThrownBy(() -> userWalletLogService.createAndSaveLog(trade, testWallet))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("不正な通貨コード");
	}

	@Test
	@DisplayName("T-504: WalletとTradeのユーザーが不一致なら例外が発生する")
	void testCreateLog_userMismatch_shouldThrow() {

		//testuserのtradeを作成
		Trade trade = createTrade(1, "JPY", new BigDecimal("100"));

		//testUser2のwalletと比較
		assertThatThrownBy(() -> userWalletLogService.createAndSaveLog(trade, testWallet2))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("所有者が一致しません");
	}
}
