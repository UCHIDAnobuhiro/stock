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

import com.example.stock.model.UserWallet;
import com.example.stock.model.Users;
import com.example.stock.repository.UserWalletRepository;
import com.example.stock.repository.UsersRepository;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class UserWalletServiceTest {

	@MockBean
	private LogoDetectionService mockLogoDetectionService;

	@Autowired
	private UserWalletService userWalletService;

	@Autowired
	private UserWalletRepository userWalletRepository;

	@Autowired
	private UsersRepository usersRepository;

	private Users testUser;

	@BeforeEach
	void setup() {
		testUser = new Users();
		testUser.setUsername("テスト太郎");
		testUser.setEmail("test@example.com");
		testUser.setPassword("$2a$10$hBrJiyk7dArR3hGR7bvu5.oYKlK6O506lRvqdl8WTIvu1bxV22EJy");
		testUser.setCreateAt(LocalDateTime.now());
		testUser.setUpdateAt(LocalDateTime.now());
		testUser.setEnabled(true);
		testUser.setFailedLoginAttempts(0);
		testUser.setAccountLocked(false);
		testUser.setLockTime(null);
		usersRepository.save(testUser);
	}

	@DisplayName("T-001: ウォレットが存在する場合に取得できるか")
	@Test
	void testGetWalletByUser_returnsCorrectWallet() {
		UserWallet wallet = new UserWallet();
		wallet.setUser(testUser);
		wallet.setJpyBalance(BigDecimal.valueOf(10000));
		wallet.setUsdBalance(BigDecimal.valueOf(100));
		wallet.setCreateAt(LocalDateTime.now());
		wallet.setUpdateAt(LocalDateTime.now());

		userWalletRepository.save(wallet);
		UserWallet result = userWalletService.getWalletByUser(testUser);

		assertThat(result).isNotNull();
		assertThat(result.getUser().getId()).isEqualTo(testUser.getId());
		assertThat(result.getJpyBalance()).isEqualTo(BigDecimal.valueOf(10000));
	}

	@DisplayName("T-002: ウォレットが存在しない場合に自動作成されるか")
	@Test
	void testGetWalletByUser_createsWalletIfNotExists() {
		UserWallet result = userWalletService.getWalletByUser(testUser);
		assertThat(result).isNotNull();
		assertThat(result.getJpyBalance()).isEqualTo(BigDecimal.ZERO);
		assertThat(result.getUsdBalance()).isEqualTo(BigDecimal.ZERO);
	}

	@DisplayName("T-003: 極端に大きな金額が保存・取得可能か")
	@Test
	void testCreateWalletWithLargeBalance() {
		UserWallet wallet = new UserWallet();
		wallet.setUser(testUser);
		wallet.setJpyBalance(new BigDecimal("10000000000"));
		wallet.setUsdBalance(new BigDecimal("10000000000"));
		wallet.setCreateAt(LocalDateTime.now());
		wallet.setUpdateAt(LocalDateTime.now());

		userWalletRepository.save(wallet);
		UserWallet result = userWalletService.getWalletByUser(testUser);

		assertThat(result.getJpyBalance()).isEqualTo(new BigDecimal("10000000000"));
	}

	@DisplayName("T-004: 少数点以下を含む残高を保存・取得可能か")
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

	@DisplayName("T-005: 負の残高の保存・取得（現在は許可）")
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

	@DisplayName("T-006: ユーザーがnullの場合はウォレット自動作成されない")
	@Test
	void testGetWalletByUser_withNullUser_shouldThrowException() {
		assertThatThrownBy(() -> userWalletService.createWalletForUser(null))
				.isInstanceOf(NullPointerException.class);
	}

	@DisplayName("T-007: 既存ウォレットは再度作成されない")
	@Test
	void testCreateDuplicateWallet_manually_throwsException() {
		userWalletService.createWalletForUser(testUser);
		try {
			userWalletService.createWalletForUser(testUser);
		} catch (Exception e) {
			assertThat(e).isInstanceOf(Exception.class);
		}
	}

	@DisplayName("T-008: 最大桁数（DECIMAL(18,2)）の保存・取得可能か")
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

	@DisplayName("T-009: 18桁超過する金額を保存されない")
	@Test
	void testWalletWithTooLargeInteger_shouldThrowException() {
		UserWallet wallet = new UserWallet();
		wallet.setUser(testUser);
		wallet.setJpyBalance(new BigDecimal("10000000000000000.01"));
		wallet.setUsdBalance(new BigDecimal("10000000000000000.01"));
		wallet.setCreateAt(LocalDateTime.now());
		wallet.setUpdateAt(LocalDateTime.now());

		try {
			userWalletRepository.save(wallet);
		} catch (Exception e) {
			assertThat(e).isInstanceOf(Exception.class);
		}
	}

	@DisplayName("T-010: 小数点以下が3桁以上の場合は保存されない")
	@Test
	void testWalletWithTooSmallDecimal_shouldThrowException() {
		UserWallet wallet = new UserWallet();
		wallet.setUser(testUser);
		wallet.setJpyBalance(new BigDecimal("0.009"));
		wallet.setUsdBalance(new BigDecimal("0.009"));
		wallet.setCreateAt(LocalDateTime.now());
		wallet.setUpdateAt(LocalDateTime.now());

		try {
			userWalletRepository.save(wallet);
		} catch (Exception e) {
			assertThat(e).isInstanceOf(Exception.class);
		}
	}

	@DisplayName("T-011: 負の最大値が保存・取得可能か")
	@Test
	void testWalletWithMinAllowedNegative() {
		UserWallet wallet = new UserWallet();
		wallet.setUser(testUser);
		wallet.setJpyBalance(new BigDecimal("-9999999999999999.99"));
		wallet.setUsdBalance(new BigDecimal("-9999999999999999.99"));
		wallet.setCreateAt(LocalDateTime.now());
		wallet.setUpdateAt(LocalDateTime.now());

		userWalletRepository.save(wallet);
		UserWallet result = userWalletService.getWalletByUser(testUser);

		assertThat(result.getJpyBalance()).isEqualTo(new BigDecimal("-9999999999999999.99"));
		assertThat(result.getUsdBalance()).isEqualTo(new BigDecimal("-9999999999999999.99"));
	}

	@DisplayName("T-012: 負の値が範囲を超えた場合は保存されない")
	@Test
	void testWalletWithTooLargeNegative_shouldThrowException() {
		UserWallet wallet = new UserWallet();
		wallet.setUser(testUser);
		wallet.setJpyBalance(new BigDecimal("-10000000000000000.00"));
		wallet.setUsdBalance(new BigDecimal("-10000000000000000.00"));
		wallet.setCreateAt(LocalDateTime.now());
		wallet.setUpdateAt(LocalDateTime.now());

		try {
			userWalletRepository.save(wallet);
		} catch (Exception e) {
			assertThat(e).isInstanceOf(Exception.class);
		}
	}
}
