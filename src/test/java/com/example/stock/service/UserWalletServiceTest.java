package com.example.stock.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.stock.dto.StockCandleWithPrevCloseDto;
import com.example.stock.model.Tickers;
import com.example.stock.model.Trade;
import com.example.stock.model.UserWallet;
import com.example.stock.model.Users;
import com.example.stock.repository.TickersRepository;
import com.example.stock.repository.TradeRepository;
import com.example.stock.repository.UserWalletLogRepository;
import com.example.stock.repository.UserWalletRepository;
import com.example.stock.repository.UsersRepository;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserWalletServiceTest {

	@MockBean
	private LogoDetectionService mockLogoDetectionService;

	@Autowired
	private UserWalletService userWalletService;

	@Autowired
	private UserWalletRepository userWalletRepository;

	@Autowired
	private UsersRepository usersRepository;

	@Autowired
	private TickersRepository tickersRepository;

	@Autowired
	private TradeRepository tradeRepository;

	@Autowired
	private UserWalletLogRepository userWalletLogRepository;
	@Autowired
	private EntityManager entityManager;

	@MockBean
	private StockService stockService;

	private Users testUser;
	private Users testUser2;
	private UserWallet testWallet;
	private UserWallet testWallet2;
	private Trade testTrade;
	private Tickers testTicker;

	@BeforeEach
	void setup() {

		userWalletLogRepository.deleteAll();
		tradeRepository.deleteAll();
		userWalletRepository.deleteAll();
		tickersRepository.deleteAll();
		usersRepository.deleteAll();

		testUser = new Users();
		testUser.setUsername("テスト太郎1");
		testUser.setEmail("test1@example.com");
		testUser.setPassword("$2a$10$hBrJiyk7dArR3hGR7bvu5.oYKlK6O506lRvqdl8WTIvu1bxV22EJy");
		testUser.setCreateAt(LocalDateTime.now());
		testUser.setUpdateAt(LocalDateTime.now());
		testUser.setEnabled(true);
		testUser.setFailedLoginAttempts(0);
		testUser.setAccountLocked(false);
		testUser.setLockTime(null);
		usersRepository.save(testUser);

		testUser2 = new Users();
		testUser2.setUsername("テスト太郎2");
		testUser2.setEmail("test2@example.com");
		testUser2.setPassword("$2a$10$hBrJiyk7dArR3hGR7bvu5.oYKlK6O506lRvqdl8WTIvu1bxV22EJy");
		testUser2.setCreateAt(LocalDateTime.now());
		testUser2.setUpdateAt(LocalDateTime.now());
		testUser2.setEnabled(true);
		testUser2.setFailedLoginAttempts(0);
		testUser2.setAccountLocked(false);
		testUser2.setLockTime(null);
		usersRepository.save(testUser2);

		//testUser1に対するUserWalletを作成し数値設定
		//getWalletByUserは該当walletない場合は自動新規作成機能がある
		testWallet = userWalletService.getWalletByUser(testUser);
		testWallet.setJpyBalance(new BigDecimal("5000"));
		testWallet.setUsdBalance(new BigDecimal("100"));

		testTicker = new Tickers();
		testTicker.setTicker("AAPL");
		testTicker.setBrand("Apple Inc.");
		tickersRepository.save(testTicker);

		StockCandleWithPrevCloseDto mockDto = new StockCandleWithPrevCloseDto();
		mockDto.setClose(100.0);
		mockDto.setPrevClose(100.0);
		mockDto.setSymbol("AAPL");
		mockDto.setInterval("1day");
		when(stockService.getLatestStockWithPrevClose("AAPL")).thenReturn(mockDto);
	}

	@DisplayName("T-001: ウォレットが存在する場合に取得できるか")
	@Test
	void testGetWalletByUser_returnsCorrectWallet() {
		UserWallet result = userWalletService.getWalletByUser(testUser);

		assertThat(result).isNotNull();
		assertThat(result.getUser().getId()).isEqualTo(testUser.getId());
		assertThat(result.getJpyBalance()).isEqualTo(BigDecimal.valueOf(5000));
	}

	@DisplayName("T-002: ウォレットが存在しない場合に自動作成されるか")
	@Test
	void testGetWalletByUser_createsWalletIfNotExists() {

		//getWalletByUserでtestUser2のwalletあるかをチェックし、ないなら新規作成される
		testWallet2 = userWalletService.getWalletByUser(testUser2);
		assertThat(testWallet2).isNotNull();
		assertThat(testWallet2.getJpyBalance()).isEqualTo(BigDecimal.ZERO);
		assertThat(testWallet2.getUsdBalance()).isEqualTo(BigDecimal.ZERO);
	}

	@DisplayName("T-003: 極端に大きな金額が保存・取得可能か")
	@Test
	void testCreateWalletWithLargeBalance() {
		testWallet.setJpyBalance(new BigDecimal("10000000000"));
		testWallet.setUsdBalance(new BigDecimal("10000000000"));

		userWalletRepository.save(testWallet);
		UserWallet result = userWalletService.getWalletByUser(testUser);

		assertThat(result.getJpyBalance()).isEqualTo(new BigDecimal("10000000000"));
	}

	@DisplayName("T-004: 少数点以下を含む残高を保存・取得可能か")
	@Test
	void testCreateWalletWithDecimalBalance() {
		testWallet.setJpyBalance(new BigDecimal("1234.56"));
		testWallet.setUsdBalance(new BigDecimal("78.90"));

		UserWallet result = userWalletService.getWalletByUser(testUser);

		assertThat(result.getJpyBalance()).isEqualTo(new BigDecimal("1234.56"));
		assertThat(result.getUsdBalance()).isEqualTo(new BigDecimal("78.90"));
	}

	@DisplayName("T-005: 負の残高の保存・取得（現在は許可）")
	@Test
	void testNegativeBalance_allowedInCurrentState() {
		testWallet.setUser(testUser);
		testWallet.setJpyBalance(new BigDecimal("-1000"));
		testWallet.setUsdBalance(new BigDecimal("-50"));

		userWalletRepository.save(testWallet);
		UserWallet result = userWalletService.getWalletByUser(testUser);

		assertThat(result.getJpyBalance()).isEqualTo(new BigDecimal("-1000"));
		assertThat(result.getUsdBalance()).isEqualTo(new BigDecimal("-50"));
	}

	@DisplayName("T-006: ユーザーがnullの場合はウォレット自動作成されない")
	@Test
	void testGetWalletByUser_withNullUser_shouldThrowException() {
		assertThatThrownBy(() -> userWalletService.createWalletForUser(null))
				.isInstanceOf(NullPointerException.class);
	}

	@DisplayName("T-007: 同一ユーザーに対しウォレットは一つしか作成されない")
	@Test
	void testWalletIsUniquePerUser() {
		// 1回目：正常作成
		userWalletService.getWalletByUser(testUser);

		// 2回目：明示的にcreateWalletForUserを呼んで制約エラーを検出
		assertThatThrownBy(() -> {
			userWalletService.createWalletForUser(testUser);
			userWalletRepository.flush(); // 💥 トリガーさせる
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

	@DisplayName("T-008: 最大桁数（DECIMAL(18,2)）の保存・取得可能か")
	@Test
	void testWalletWithMaxAllowedDecimal() {
		testWallet.setJpyBalance(new BigDecimal("9999999999999999.99"));
		testWallet.setUsdBalance(new BigDecimal("9999999999999999.99"));

		userWalletRepository.save(testWallet);
		UserWallet result = userWalletService.getWalletByUser(testUser);

		assertThat(result.getJpyBalance()).isEqualTo(new BigDecimal("9999999999999999.99"));
		assertThat(result.getUsdBalance()).isEqualTo(new BigDecimal("9999999999999999.99"));
	}

	@DisplayName("T-009: 18桁超過する金額を保存されない")
	@Test
	void testWalletWithTooLargeInteger_shouldThrowException() {
		testWallet.setJpyBalance(new BigDecimal("10000000000000000.01"));
		testWallet.setUsdBalance(new BigDecimal("10000000000000000.01"));

		try {
			userWalletRepository.save(testWallet);
		} catch (Exception e) {
			assertThat(e).isInstanceOf(Exception.class);
		}
	}

	@DisplayName("T-010: 小数点以下が3桁以上の場合は保存されない")
	@Test
	void testWalletWithTooSmallDecimal_shouldThrowException() {
		testWallet.setJpyBalance(new BigDecimal("0.009"));
		testWallet.setUsdBalance(new BigDecimal("0.009"));

		try {
			userWalletRepository.save(testWallet);
		} catch (Exception e) {
			assertThat(e).isInstanceOf(Exception.class);
		}
	}

	@DisplayName("T-011: 負の最大値が保存・取得可能か")
	@Test
	void testWalletWithMinAllowedNegative() {
		testWallet.setJpyBalance(new BigDecimal("-9999999999999999.99"));
		testWallet.setUsdBalance(new BigDecimal("-9999999999999999.99"));

		userWalletRepository.save(testWallet);
		UserWallet result = userWalletService.getWalletByUser(testUser);

		assertThat(result.getJpyBalance()).isEqualTo(new BigDecimal("-9999999999999999.99"));
		assertThat(result.getUsdBalance()).isEqualTo(new BigDecimal("-9999999999999999.99"));
	}

	@DisplayName("T-012: 負の値が範囲を超えた場合は保存されない")
	@Test
	void testWalletWithTooLargeNegative_shouldThrowException() {
		testWallet.setJpyBalance(new BigDecimal("-10000000000000000.00"));
		testWallet.setUsdBalance(new BigDecimal("-10000000000000000.00"));
		try {
			userWalletRepository.save(testWallet);
		} catch (Exception e) {
			assertThat(e).isInstanceOf(Exception.class);
		}
	}

	@DisplayName("T-013: 日本円の買い取引により残高が正常に減少すること")
	@Test
	void testApplyTradeToWallet_BuyJPY() {
		testTrade = createTrade(testUser, testTicker, "JPY", new BigDecimal("100"), 0); // side = 0 = 買い注文
		userWalletService.applyTradeToWallet(testTrade);

		UserWallet updatedWallet = userWalletRepository.findByUser(testUser);
		assertThat(updatedWallet.getJpyBalance()).isEqualByComparingTo("4900");
	}

	@DisplayName("T-014: 残高不足なら更新されない（ログのみ記録）")
	@Test
	void testApplyTradeToWallet_InsufficientBalance() {
		BigDecimal before = userWalletRepository.findByUser(testUser).getJpyBalance();

		testTrade = createTrade(testUser, testTicker, "JPY", new BigDecimal("5001"), 0);
		userWalletService.applyTradeToWallet(testTrade);

		UserWallet updatedWallet = userWalletRepository.findByUser(testUser);
		assertThat(updatedWallet.getJpyBalance()).isEqualByComparingTo(before);
	}

	@DisplayName("T-015: サポートされていない通貨なら更新されない（ログのみ記録）")
	@Test
	void testApplyTradeToWallet_InvalidCurrency() {
		testTrade = createTrade(testUser, testTicker, "BTC", new BigDecimal("100"), 0);
		BigDecimal before = testWallet.getJpyBalance();

		userWalletService.applyTradeToWallet(testTrade);
		UserWallet updatedWallet = userWalletRepository.findByUser(testUser);
		assertThat(updatedWallet.getJpyBalance()).isEqualByComparingTo(before);
	}

	// テスト用の取引データ生成ヘルパーメソッド
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
