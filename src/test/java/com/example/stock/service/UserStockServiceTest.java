package com.example.stock.service;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.stock.model.Tickers;
import com.example.stock.model.UserStock;
import com.example.stock.model.Users;
import com.example.stock.repository.TickersRepository;
import com.example.stock.repository.UserStockRepository;
import com.example.stock.repository.UsersRepository;

/**
 * UserStockService の単体テスト。
 * ユーザーが保有する銘柄（株式）の数量を正しく取得できるかを検証する。
 */
@SpringBootTest
@Transactional
public class UserStockServiceTest {

	@Autowired
	private UserStockService userStockService;

	@Autowired
	private UserStockRepository userStockRepository;

	@Autowired
	private UsersRepository usersRepository;

	@Autowired
	private TickersRepository tickersRepository;

	private Users testUser;
	private Tickers testTicker;

	/**
	 * 各テスト実行前に必要なユーザーと銘柄データを用意する。
	 */
	@BeforeEach
	void setup() {
		// ユーザー作成
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

		// 銘柄作成（例: Apple の株）
		testTicker = new Tickers();
		testTicker.setTicker("AAPL");
		testTicker.setBrand("Apple Inc.");
		tickersRepository.save(testTicker);
	}

	/**
	 * createAt / updateAt をセットした UserStock を生成する共通メソッド
	 */
	private UserStock buildStock(Users user, Tickers ticker, BigDecimal quantity) {
		UserStock stock = new UserStock();
		stock.setUser(user);
		stock.setTicker(ticker);
		stock.setQuantity(quantity);
		stock.setCreateAt(LocalDateTime.now());
		stock.setUpdateAt(LocalDateTime.now());
		return stock;
	}

	/**
	 * ユーザーが株式を保有していない場合、数量0が返るかテスト
	 */
	@Test
	void testGetStockQuantity_returnsZeroIfNoStock() {
		BigDecimal quantity = userStockService.getStockQuantityByUserAndTicker(testUser, "AAPL");
		assertThat(quantity).isEqualTo(BigDecimal.ZERO);
	}

	/**
	 * ユーザーが株式を保有している場合、その正しい数量が返るかテスト
	 */
	@Test
	void testGetStockQuantity_returnsCorrectQuantity() {
		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("42.75")));
		BigDecimal quantity = userStockService.getStockQuantityByUserAndTicker(testUser, "AAPL");
		assertThat(quantity).isEqualTo(new BigDecimal("42.75"));
	}

	/**
	 * 存在しない ticker シンボルを指定した場合の挙動確認。
	 */
	@Test
	void testGetStockQuantity_withInvalidSymbol_shouldThrowOrReturnZero() {
		assertThatThrownBy(() -> userStockService.getStockQuantityByUserAndTicker(testUser, "INVALID"))
				.isInstanceOf(RuntimeException.class);
	}

	/**
	 * 最大数量（DECIMAL(18,2)）のテスト：9999999999999999.99
	 */
	@Test
	void testGetStockQuantity_withMaxValue() {
		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("9999999999999999.99")));
		BigDecimal quantity = userStockService.getStockQuantityByUserAndTicker(testUser, "AAPL");
		assertThat(quantity).isEqualTo(new BigDecimal("9999999999999999.99"));
	}

	/**
	 * 最小有効数量（0.01）を扱えるか確認
	 */
	@Test
	void testGetStockQuantity_withMinPositiveValue() {
		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("0.01")));
		BigDecimal quantity = userStockService.getStockQuantityByUserAndTicker(testUser, "AAPL");
		assertThat(quantity).isEqualTo(new BigDecimal("0.01"));
	}

	/**
	 * 小数点以下の数量を正確に取得できるかテスト
	 */
	@Test
	void testGetStockQuantity_withDecimalQuantity() {
		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("1.23")));
		BigDecimal quantity = userStockService.getStockQuantityByUserAndTicker(testUser, "AAPL");
		assertThat(quantity).isEqualTo(new BigDecimal("1.23"));
	}

	/**
	 * 負の数量を許容する（現状ルールでは許可されている）
	 */
	@Test
	void testGetStockQuantity_withNegativeQuantity() {
		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("-50")));
		BigDecimal quantity = userStockService.getStockQuantityByUserAndTicker(testUser, "AAPL");
		assertThat(quantity).isEqualTo(new BigDecimal("-50"));
	}

	/**
	 * 複数のユーザーが同じ銘柄を保有しても干渉しないことを確認
	 */
	@Test
	void testGetStockQuantity_multiUserSeparation() {
		Users userB = new Users();
		userB.setUsername("株式次郎");
		userB.setEmail("second@example.com");
		userB.setPassword("pass");
		userB.setCreateAt(LocalDateTime.now());
		userB.setUpdateAt(LocalDateTime.now());
		usersRepository.save(userB);

		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("100")));
		userStockRepository.save(buildStock(userB, testTicker, new BigDecimal("200")));

		BigDecimal aQuantity = userStockService.getStockQuantityByUserAndTicker(testUser, "AAPL");
		BigDecimal bQuantity = userStockService.getStockQuantityByUserAndTicker(userB, "AAPL");

		assertThat(aQuantity).isEqualTo(new BigDecimal("100"));
		assertThat(bQuantity).isEqualTo(new BigDecimal("200"));
	}

	/**
	 * ユーザーが複数の銘柄を持っていても、個別に数量を取得できることをテスト
	 */
	@Test
	void testGetStockQuantity_multiTickerSeparation() {
		Tickers goog = new Tickers();
		goog.setTicker("GOOG");
		goog.setBrand("Google LLC");
		tickersRepository.save(goog);

		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("10"))); // AAPL
		userStockRepository.save(buildStock(testUser, goog, new BigDecimal("5"))); // GOOG

		BigDecimal quantityAAPL = userStockService.getStockQuantityByUserAndTicker(testUser, "AAPL");
		BigDecimal quantityGOOG = userStockService.getStockQuantityByUserAndTicker(testUser, "GOOG");

		assertThat(quantityAAPL).isEqualTo(new BigDecimal("10"));
		assertThat(quantityGOOG).isEqualTo(new BigDecimal("5"));
	}
}
