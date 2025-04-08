package com.example.stock.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.transaction.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.stock.enums.TokenType;
import com.example.stock.exception.UserRegistrationException;
import com.example.stock.model.UserToken;
import com.example.stock.model.Users;
import com.example.stock.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsersService {

	private final UsersRepository usersRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserTokenService userTokenService;
	private final MailService mailService;

	@Transactional
	public void registerUser(Users user) {
		validateUser(user);
		saveUserAndSendVerification(user);
	}

	/**
	 * ユーザー登録時の入力値バリデーションを行う。
	 * 問題があれば UserRegistrationException をスローする。
	 */
	private void validateUser(Users user) {
		// ユーザー名が空かどうかをチェック
		if (user.getUsername() == null || user.getUsername().isBlank()) {
			throw new UserRegistrationException("name", "名前を入力してください");
		}

		// メールアドレスが空かどうかをチェック
		if (user.getEmail() == null || user.getEmail().isBlank()) {
			throw new UserRegistrationException("email", "メールアドレスを入力してください");
		}

		// メールアドレスが既に登録されていないかをチェック
		String email = user.getEmail().trim();
		if (usersRepository.findByEmail(email).isPresent()) {
			throw new UserRegistrationException("email", "このメールアドレスは既に登録されています");
		}

		// パスワードまたは確認用パスワードが未入力かどうかをチェック
		if (user.getPassword() == null || user.getConfirmPassword().isBlank() || user.getConfirmPassword() == null
				|| user.getConfirmPassword().isBlank()) {
			throw new UserRegistrationException("password", "パスワードを入力してください");
		}

		// パスワードと確認用パスワードが一致するかをチェック
		if (!user.getPassword().equals(user.getConfirmPassword())) {
			throw new UserRegistrationException("confirmPassword", "パスワードが一致しません");
		}
	}

	/**
	 * ユーザー情報を保存し、メール認証用のトークンを生成・送信する処理。
	 */
	private void saveUserAndSendVerification(Users user) {
		// 現在の時刻を表示
		LocalDateTime now = LocalDateTime.now();

		// ユーザーの作成日時・更新日時を現在時刻でセット
		user.setCreateAt(now);
		user.setUpdateAt(now);

		// パスワードをハッシュ化して保存（セキュリティのため）
		user.setPassword(passwordEncoder.encode(user.getPassword()));

		// アカウントの有効化状態を false に（メール認証後に有効化される）
		user.setEnabled(false);

		// ユーザーをデータベースに保存
		usersRepository.save(user);

		// 認証トークンを作成
		UserToken token = userTokenService.createToken(user, TokenType.VERIFY_EMAIL, Duration.ofHours(24));

		// 認証用メールを送信
		mailService.sendVerificationEmail(user.getEmail(), token.getToken(), TokenType.VERIFY_EMAIL);
	}

	/**
	 * トークンを使ってユーザーの認証を行う。
	 * - トークンが有効であればユーザーを有効化し、トークンを削除する。
	 * - トークンが無効または期限切れの場合は false を返す。
	 *
	 * @param token メール認証用のトークン
	 * @return 認証成功なら true、失敗なら false
	 */
	@Transactional
	public boolean verifyUser(String tokenStr) {
		// トークンが存在するかデータベースから検索
		Optional<UserToken> optionalToken = userTokenService.validateToken(tokenStr, TokenType.VERIFY_EMAIL);
		// トークンが見つからなければ認証失敗
		if (optionalToken.isEmpty())
			return false;

		UserToken token = optionalToken.get();
		Users user = token.getUser();

		// 既に有効化済みなら何もしない
		if (user.isEnabled()) {
			userTokenService.deleteToken(user, TokenType.VERIFY_EMAIL);
			return true;
		}

		// トークンに紐づくユーザーを取得し、有効化（enabled = true）に設定
		user.setEnabled(true);
		// ユーザー情報を更新して保存
		usersRepository.save(user);
		// トークンを削除（再利用を防ぐため）
		userTokenService.deleteToken(user, TokenType.VERIFY_EMAIL);
		return true;
	}

	/**
	 * パスワード再設定用トークンの有効性を検証するメソッド。
	 *
	 * @param tokenStr 検証対象のトークン文字列
	 * @return トークンが存在し、かつ期限内かどうか（有効なら true、無効なら false）
	 */
	@Transactional
	public boolean validateResetPasswordToken(String tokenStr) {
		Optional<UserToken> verificationToken = userTokenService.validateToken(tokenStr, TokenType.RESET_PASSWORD);
		if (verificationToken.isEmpty()) {
			return false;
		}

		UserToken token = verificationToken.get();

		// トークンが期限切れだったら false
		if (token.isExpired()) {
			return false;
		}

		// ここまで来たら有効なRESET_PASSWORDトークン
		return true;

	}

	/**
	 * ユーザーに認証／パスワード再設定用のメールを再送信する。
	 *
	 * @param email 対象ユーザーのメールアドレス
	 * @param tokenType トークンの種別（VERIFY_EMAIL or RESET_PASSWORD）
	 * @throws UserRegistrationException 条件に合わない場合にスロー
	 */
	@Transactional
	public void resendVerificationEmail(String email, TokenType tokenType) {
		// emailをトリムする
		String trimmedEmail = email.trim();
		Users user = usersRepository.findByEmail(trimmedEmail)
				.orElseThrow(() -> new UserRegistrationException("email", "このメールアドレスは登録されていません"));

		if (user.isEnabled() && tokenType == TokenType.VERIFY_EMAIL) {
			throw new UserRegistrationException("email", "このメールアドレスは既に認証されています");
		}

		// 古いトークンがある場合は削除する
		userTokenService.deleteToken(user, tokenType);

		// 新しいトークンを発行
		UserToken token = userTokenService.createToken(user, tokenType, Duration.ofHours(24));

		// 認証メールを再送信
		mailService.sendVerificationEmail(user.getEmail(), token.getToken(), tokenType);
	}

	/**
	 * パスワード再設定トークンからユーザー情報を取得する。
	 *
	 * @param token トークン文字列
	 * @return 有効なトークンに紐づくユーザー（トークンが無効または期限切れの場合は empty）
	 */
	@Transactional
	public Optional<Users> getUserFromResetToken(String token) {
		Optional<UserToken> verificationToken = userTokenService.validateToken(token, TokenType.RESET_PASSWORD);

		if (verificationToken.isEmpty()) {
			return Optional.empty();
		}

		UserToken userToken = verificationToken.get();
		if (userToken.isExpired()) {
			return Optional.empty();
		}

		return Optional.of(userToken.getUser());
	}

	/**
	 * トークンを使ってユーザーのパスワードをリセットする。
	 *
	 * @param token パスワード再設定用トークン
	 * @param rawPassword ユーザーが新たに入力したパスワード（平文）
	 * @return パスワードの更新が成功した場合は true、トークンが無効または期限切れなら false
	 */
	@Transactional
	public boolean resetPassword(String token, String rawPassword) {
		// トークンの取得＆有効性チェック
		Optional<UserToken> tokenOpt = userTokenService.validateToken(token, TokenType.RESET_PASSWORD);

		if (tokenOpt.isEmpty() || tokenOpt.get().isExpired()) {
			return false;
		}

		UserToken userToken = tokenOpt.get();
		Users user = userToken.getUser();

		// パスワードをエンコードして保存
		String encodedPassword = passwordEncoder.encode(rawPassword);
		user.setPassword(encodedPassword);

		// アカウントロック解除
		if (user.isAccountLocked()) {
			user.setAccountLocked(false);
			user.setFailedLoginAttempts(0);
			user.setLockTime(null);
		}

		// もしアカウント有効化してない場合は有効化
		if (!user.isEnabled()) {
			user.setEnabled(true);
		}

		// update_at を更新
		user.setUpdateAt(LocalDateTime.now());
		usersRepository.save(user);

		// トークンを削除または無効化
		userTokenService.deleteToken(user, TokenType.RESET_PASSWORD);

		return true;
	}

	public boolean isLockExpired(Users user) {
		if (user.getLockTime() == null)
			return true;
		LocalDateTime lockTime = user.getLockTime();
		return lockTime.plusMinutes(5).isBefore(LocalDateTime.now());
	}

}
