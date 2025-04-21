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
	private Users testUser2;
	private Tickers testTicker;
	private Tickers testTicker2;

	/**
	 * 各テスト実行前に必要なユーザーと銘柄データを用意する。
	 */
	@BeforeEach
	void setup() {
		//重複防止のためデータベースをリセット
		userStockRepository.deleteAll();
		tickersRepository.deleteAll();
		usersRepository.deleteAll();

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

		// 銘柄作成（例: Apple の株）
		testTicker = new Tickers();
		testTicker.setTicker("AAPL");
		testTicker.setBrand("Apple Inc.");
		tickersRepository.save(testTicker);

		testTicker2 = new Tickers();
		testTicker2.setTicker("GOOG");
		testTicker2.setBrand("Google LLC");
		tickersRepository.save(testTicker2);
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
	 * T-101ユーザーが株式を保有していない場合、数量0が返るかテスト
	 */
	@Test
	void testGetStockQuantity_returnsZeroIfNoStock() {
		BigDecimal quantity = userStockService.getStockQuantityByUserAndTicker(testUser, testTicker.getTicker());
		assertThat(quantity).isEqualTo(BigDecimal.ZERO);
	}

	/**
	 * T-102ユーザーが株式を保有している場合、その正しい数量が返るかテスト
	 */
	@Test
	void testGetStockQuantity_returnsCorrectQuantity() {
		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("42.75")));
		BigDecimal quantity = userStockService.getStockQuantityByUserAndTicker(testUser, testTicker.getTicker());
		assertThat(quantity).isEqualTo(new BigDecimal("42.75"));
	}

	/**
	 * T-103存在しない ticker シンボルを指定した場合0が返す。
	 */
	@Test
	void testGetStockQuantity_withInvalidSymbol_shouldThrowOrReturnZero() {
		BigDecimal quantity = userStockService.getStockQuantityByUserAndTicker(testUser, "INVALID");
		assertThat(quantity).isEqualTo(BigDecimal.ZERO);
	}

	/**
	 * T-104最大数量（DECIMAL(18,2)）のテスト：9999999999999999.99を扱えるか確認
	 */
	@Test
	void testGetStockQuantity_withMaxValue() {
		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("9999999999999999.99")));
		BigDecimal quantity = userStockService.getStockQuantityByUserAndTicker(testUser, testTicker.getTicker());
		assertThat(quantity).isEqualTo(new BigDecimal("9999999999999999.99"));
	}

	/**
	 * T-105最小有効数量（0.01）を扱えるか確認
	 */
	@Test
	void testGetStockQuantity_withMinPositiveValue() {
		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("0.01")));
		BigDecimal quantity = userStockService.getStockQuantityByUserAndTicker(testUser, testTicker.getTicker());
		assertThat(quantity).isEqualTo(new BigDecimal("0.01"));
	}

	/**
	 * T-106負の数量を許容する（現状ルールでは許可されている）
	 */
	@Test
	void testGetStockQuantity_withNegativeQuantity() {
		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("-50")));
		BigDecimal quantity = userStockService.getStockQuantityByUserAndTicker(testUser, testTicker.getTicker());
		assertThat(quantity).isEqualTo(new BigDecimal("-50"));
	}

	/**
	 * T-107複数のユーザーが同じ銘柄を保有しても干渉しないことを確認
	 */
	@Test
	void testGetStockQuantity_multiUserSeparation() {
		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("100")));
		userStockRepository.save(buildStock(testUser2, testTicker, new BigDecimal("200")));

		BigDecimal aQuantity = userStockService.getStockQuantityByUserAndTicker(testUser, testTicker.getTicker());
		BigDecimal bQuantity = userStockService.getStockQuantityByUserAndTicker(testUser2, testTicker.getTicker());

		assertThat(aQuantity).isEqualTo(new BigDecimal("100"));
		assertThat(bQuantity).isEqualTo(new BigDecimal("200"));
	}

	/**
	 * T-108ユーザーが複数の銘柄を持っていても、個別に数量を取得できることをテスト
	 */
	@Test
	void testGetStockQuantity_multiTickerSeparation() {
		userStockRepository.save(buildStock(testUser, testTicker, new BigDecimal("10"))); // AAPL
		userStockRepository.save(buildStock(testUser, testTicker2, new BigDecimal("5"))); // GOOG

		BigDecimal quantityAAPL = userStockService.getStockQuantityByUserAndTicker(testUser, testTicker.getTicker());
		BigDecimal quantityGOOG = userStockService.getStockQuantityByUserAndTicker(testUser, testTicker2.getTicker());

		assertThat(quantityAAPL).isEqualTo(new BigDecimal("10"));
		assertThat(quantityGOOG).isEqualTo(new BigDecimal("5"));
	}
}
