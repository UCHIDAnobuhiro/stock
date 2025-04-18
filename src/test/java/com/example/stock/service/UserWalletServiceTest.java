package com.example.stock.service;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.example.stock.model.UserWallet;
import com.example.stock.model.Users;
import com.example.stock.repository.UserWalletRepository;
import com.example.stock.repository.UsersRepository;

/**
 * UserWalletService の単体テストクラス。
 * ユーザーウォレット取得・作成のロジックが正しく動作するかを検証する。
 */
@SpringBootTest
@Transactional // 各テスト実行後にデータベースをロールバックすることで、テスト間の影響を防ぐ
public class UserWalletServiceTest {

	@Autowired
	private UserWalletService userWalletService;

	@Autowired
	private UserWalletRepository userWalletRepository;

	@Autowired
	private UsersRepository usersRepository;

	private Users testUser;

	/**
	 * 各テストメソッドの前に共通で使用するテスト用ユーザーを作成・保存する。
	 * ※ 各テストではこの testUser を利用してウォレット操作を行う。
	 */
	@BeforeEach
	void setup() {
		testUser = new Users();
		testUser.setUsername("テスト太郎");
		testUser.setEmail("test@example.com");
		testUser.setPassword("$2a$10$hBrJiyk7dArR3hGR7bvu5.oYKlK6O506lRvqdl8WTIvu1bxV22EJy"); // bcryptで暗号化されたパスワード
		testUser.setCreateAt(LocalDateTime.now());
		testUser.setUpdateAt(LocalDateTime.now());
		testUser.setEnabled(true);
		testUser.setFailedLoginAttempts(0);
		testUser.setAccountLocked(false);
		testUser.setLockTime(null);

		usersRepository.save(testUser);
	}

	/**
	 * 既にウォレットが存在しているユーザーに対して正しく取得できるかをテストする。
	 */
	@Test
	void testGetWalletByUser_returnsCorrectWallet() {
		// 事前にウォレットを作成・保存
		UserWallet wallet = new UserWallet();
		wallet.setUser(testUser);
		wallet.setJpyBalance(BigDecimal.valueOf(10000));
		wallet.setUsdBalance(BigDecimal.valueOf(100));
		wallet.setCreateAt(LocalDateTime.now());
		wallet.setUpdateAt(LocalDateTime.now());

		userWalletRepository.save(wallet);

		// ウォレット取得メソッドを呼び出し
		UserWallet result = userWalletService.getWalletByUser(testUser);

		// 正しく取得できたかを検証
		assertThat(result).isNotNull();
		assertThat(result.getUser().getId()).isEqualTo(testUser.getId());
		assertThat(result.getJpyBalance()).isEqualTo(BigDecimal.valueOf(10000));
	}

	/**
	 * ユーザーにウォレットが存在しない場合、自動で新規作成されるかをテスト。
	 */
	@Test
	void testGetWalletByUser_createsWalletIfNotExists() {
		UserWallet result = userWalletService.getWalletByUser(testUser);

		// ウォレットが作成され、初期残高が0であることを確認
		assertThat(result).isNotNull();
		assertThat(result.getUser().getId()).isEqualTo(testUser.getId());
		assertThat(result.getJpyBalance()).isEqualTo(BigDecimal.ZERO);
		assertThat(result.getUsdBalance()).isEqualTo(BigDecimal.ZERO);
	}

	/**
	 * 非常に大きな金額の残高が保存・取得できるかをテスト。
	 */
	@Test
	void testCreateWalletWithLargeBalance() {
		UserWallet wallet = new UserWallet();
		wallet.setUser(testUser);
		wallet.setJpyBalance(new BigDecimal("10000000000")); // 100億円
		wallet.setUsdBalance(new BigDecimal("10000000000"));
		wallet.setCreateAt(LocalDateTime.now());
		wallet.setUpdateAt(LocalDateTime.now());

		userWalletRepository.save(wallet);

		UserWallet result = userWalletService.getWalletByUser(testUser);

		assertThat(result.getJpyBalance()).isEqualTo(new BigDecimal("10000000000"));
	}

	/**
	 * 小数（少数点以下）を含む残高も正しく扱えるかをテスト。
	 */
	@Test
	void testCreateWalletWithDecimalBalance() {
		UserWallet wallet = new UserWallet();
		wallet.setUser(testUser);
		wallet.setJpyBalance(new BigDecimal("1234.56"));
		wallet.setUsdBalance(new BigDecimal("78.90"));
		wallet.setCreateAt(LocalDateTime.now());
		wallet.setUpdateAt(LocalDateTime.now());

		userWalletRepository.save(wallet);

		UserWallet result = userWalletService.getWalletByUser(testUser);

		assertThat(result.getJpyBalance()).isEqualTo(new BigDecimal("1234.56"));
		assertThat(result.getUsdBalance()).isEqualTo(new BigDecimal("78.90"));
	}

	/**
	 * 負の残高（現在の仕様では許容されている）を保存・取得できるかをテスト。
	 * ※将来的にルール変更（負残高禁止）される場合は見直しが必要。
	 */
	@Test
	void testNegativeBalance_allowedInCurrentState() {
		UserWallet wallet = new UserWallet();
		wallet.setUser(testUser);
		wallet.setJpyBalance(new BigDecimal("-1000"));
		wallet.setUsdBalance(new BigDecimal("-50"));
		wallet.setCreateAt(LocalDateTime.now());
		wallet.setUpdateAt(LocalDateTime.now());

		userWalletRepository.save(wallet);

		UserWallet result = userWalletService.getWalletByUser(testUser);

		assertThat(result.getJpyBalance()).isEqualTo(new BigDecimal("-1000"));
		assertThat(result.getUsdBalance()).isEqualTo(new BigDecimal("-50"));
	}

	/**
	 * getWalletByUser を複数回呼び出しても、同じウォレットが返ってくることを確認する。
	 */
	@Test
	void testCreateWalletTwice_shouldNotDuplicate() {
		UserWallet first = userWalletService.getWalletByUser(testUser);
		UserWallet second = userWalletService.getWalletByUser(testUser);

		// 同一のウォレットID（同一インスタンス）であることを確認
		assertThat(first.getId()).isEqualTo(second.getId());
	}

	/**
	 * ユーザーがnullの場合、NullPointerExceptionがスローされることをテスト。
	 */
	@Test
	void testGetWalletByUser_withNullUser_shouldThrowException() {
		assertThatThrownBy(() -> {
			userWalletService.getWalletByUser(null);
		}).isInstanceOf(NullPointerException.class);
	}

	/**
	 * 既にウォレットが存在するユーザーに対して再度作成を試みた場合、
	 * 一意制約違反で例外がスローされることを確認。
	 */
	@Test
	void testCreateDuplicateWallet_manually_throwsException() {
		userWalletService.createWalletForUser(testUser);

		assertThatThrownBy(() -> {
			userWalletService.createWalletForUser(testUser);
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

	/**
	 * 最大値（DECIMAL(18,2)）の境界値テスト：9999999999999999.99
	 */
	@Test
	void testWalletWithMaxAllowedDecimal() {
		UserWallet wallet = new UserWallet();
		wallet.setUser(testUser);
		wallet.setJpyBalance(new BigDecimal("9999999999999999.99"));
		wallet.setUsdBalance(new BigDecimal("9999999999999999.99"));
		wallet.setCreateAt(LocalDateTime.now());
		wallet.setUpdateAt(LocalDateTime.now());

		userWalletRepository.save(wallet);
		UserWallet result = userWalletService.getWalletByUser(testUser);

		assertThat(result.getJpyBalance()).isEqualTo(new BigDecimal("9999999999999999.99"));
		assertThat(result.getUsdBalance()).isEqualTo(new BigDecimal("9999999999999999.99"));
	}

	/**
	 * 整数部分が18桁を超える場合（10000000000000000.00）は例外をスローする
	 */
	@Test
	void testWalletWithTooLargeInteger_shouldThrowException() {
		UserWallet wallet = new UserWallet();
		wallet.setUser(testUser);
		wallet.setJpyBalance(new BigDecimal("10000000000000000.00")); // 18桁超過
		wallet.setUsdBalance(BigDecimal.TEN);
		wallet.setCreateAt(LocalDateTime.now());
		wallet.setUpdateAt(LocalDateTime.now());

		assertThatThrownBy(() -> userWalletRepository.save(wallet))
				.isInstanceOf(Exception.class);
	}

	/**
	 * 小数点以下が3桁以上の場合は保存できない（DECIMAL(18,2)違反）
	 */
	@Test
	void testWalletWithTooSmallDecimal_shouldThrowException() {
		UserWallet wallet = new UserWallet();
		wallet.setUser(testUser);
		wallet.setJpyBalance(new BigDecimal("0.009")); // 小数第3位
		wallet.setUsdBalance(BigDecimal.TEN);
		wallet.setCreateAt(LocalDateTime.now());
		wallet.setUpdateAt(LocalDateTime.now());

		assertThatThrownBy(() -> userWalletRepository.save(wallet))
				.isInstanceOf(Exception.class);
	}

	/**
	 * 負の最大値（-9999999999999999.99）は許容される
	 */
	@Test
	void testWalletWithMinAllowedNegative() {
		UserWallet wallet = new UserWallet();
		wallet.setUser(testUser);
		wallet.setJpyBalance(new BigDecimal("-9999999999999999.99"));
		wallet.setUsdBalance(BigDecimal.ZERO);
		wallet.setCreateAt(LocalDateTime.now());
		wallet.setUpdateAt(LocalDateTime.now());

		userWalletRepository.save(wallet);
		UserWallet result = userWalletService.getWalletByUser(testUser);

		assertThat(result.getJpyBalance()).isEqualTo(new BigDecimal("-9999999999999999.99"));
	}

	/**
	 * 負の値が範囲を超えた場合（-10000000000000000.00）は保存できない
	 */
	@Test
	void testWalletWithTooLargeNegative_shouldThrowException() {
		UserWallet wallet = new UserWallet();
		wallet.setUser(testUser);
		wallet.setJpyBalance(new BigDecimal("-10000000000000000.00"));
		wallet.setUsdBalance(BigDecimal.ZERO);
		wallet.setCreateAt(LocalDateTime.now());
		wallet.setUpdateAt(LocalDateTime.now());

		assertThatThrownBy(() -> userWalletRepository.save(wallet))
				.isInstanceOf(Exception.class);
	}

}
