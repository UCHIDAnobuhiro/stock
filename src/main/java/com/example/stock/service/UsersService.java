package com.example.stock.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import jakarta.transaction.Transactional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.stock.exception.UserRegistrationException;
import com.example.stock.model.Users;
import com.example.stock.model.VerificationToken;
import com.example.stock.repository.UsersRepository;
import com.example.stock.repository.VerificationTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsersService {
	private final UsersRepository usersRepository;
	private final PasswordEncoder passwordEncoder;
	private final VerificationTokenRepository tokenRepository;
	private final MailService mailService;

	public Users getLoggedInUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof Users) {
			return (Users) authentication.getPrincipal();
		}
		return null;
	}

	public Users getLoggedInUserOrThrow() {
		Users users = getLoggedInUser();
		if (users == null) {
			throw new IllegalStateException("ログインユーザが取得できません");
		}
		return users;
	}

	@Transactional
	public void registerUser(Users users) {
		validateUser(users);
		saveUserAndSendVerification(users);
	}

	/**
	 * ユーザー登録時の入力値バリデーションを行う。
	 * 問題があれば UserRegistrationException をスローする。
	 */
	private void validateUser(Users users) {
		// ユーザー名が空かどうかをチェック
		if (users.getUsername() == null || users.getUsername().isBlank()) {
			throw new UserRegistrationException("name", "名前を入力してください");
		}

		// メールアドレスが空かどうかをチェック
		if (users.getEmail() == null || users.getEmail().isBlank()) {
			throw new UserRegistrationException("email", "メールアドレスを入力してください");
		}

		// メールアドレスが既に登録されていないかをチェック
		String email = users.getEmail().trim();
		if (usersRepository.findByEmail(email).isPresent()) {
			throw new UserRegistrationException("email", "このメールアドレスは既に登録されています");
		}

		// パスワードまたは確認用パスワードが未入力かどうかをチェック
		if (users.getPassword() == null || users.getConfirmPassword().isBlank() || users.getConfirmPassword() == null
				|| users.getConfirmPassword().isBlank()) {
			throw new UserRegistrationException("password", "パスワードを入力してください");
		}

		// パスワードと確認用パスワードが一致するかをチェック
		if (!users.getPassword().equals(users.getConfirmPassword())) {
			throw new UserRegistrationException("confirmPassword", "パスワードが一致しません");
		}
	}

	/**
	 * ユーザー情報を保存し、メール認証用のトークンを生成・送信する処理。
	 */
	private void saveUserAndSendVerification(Users users) {
		// 現在の時刻を表示
		LocalDateTime now = LocalDateTime.now();

		// ユーザーの作成日時・更新日時を現在時刻でセット
		users.setCreateAt(now);
		users.setUpdateAt(now);

		// パスワードをハッシュ化して保存（セキュリティのため）
		users.setPassword(passwordEncoder.encode(users.getPassword()));

		// アカウントの有効化状態を false に（メール認証後に有効化される）
		users.setEnabled(false);

		// ユーザーをデータベースに保存
		usersRepository.save(users);

		// 認証トークンを生成
		String token = UUID.randomUUID().toString();
		VerificationToken verificationToken = new VerificationToken();
		verificationToken.setToken(token);
		verificationToken.setUser(users);
		verificationToken.setExpiryDate(now.plusHours(24));

		// トークンをデータベースに保存
		tokenRepository.save(verificationToken);

		// 認証用メールを送信
		mailService.sendVerificationEmail(users.getEmail(), token);
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
	public boolean verifyUser(String token) {
		// トークンが存在するかデータベースから検索
		Optional<VerificationToken> optionalToken = tokenRepository.findByToken(token);
		// トークンが見つからなければ認証失敗
		if (optionalToken.isEmpty())
			return false;

		VerificationToken verificationToken = optionalToken.get();

		// トークンの有効期限が切れている場合はトークンを削除し、認証失敗
		if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
			tokenRepository.delete(verificationToken);
			return false;
		}

		// トークンに紐づくユーザーを取得し、有効化（enabled = true）に設定
		Users user = verificationToken.getUser();
		user.setEnabled(true);
		// ユーザー情報を更新して保存
		usersRepository.save(user);
		// トークンを削除（再利用を防ぐため）
		tokenRepository.delete(verificationToken);
		return true;
	}

	@Transactional
	public void resendVerificationEmail(String email) {
		// emailをトリムする
		String trimmedEmail = email.trim();
		Users user = usersRepository.findByEmail(trimmedEmail)
				.orElseThrow(() -> new UserRegistrationException("email", "このメールアドレスは登録されていません"));

		if (user.isEnabled()) {
			throw new UserRegistrationException("email", "このメールアドレスは既に認証されています");
		}

		// 古いトークンがある場合は削除する
		tokenRepository.deleteByUser(user);

		// 新しいトークンを発行
		String token = UUID.randomUUID().toString();
		LocalDateTime expiryDate = LocalDateTime.now().plusHours(24);

		VerificationToken newToken = new VerificationToken();
		newToken.setToken(token);
		newToken.setUser(user);
		newToken.setExpiryDate(expiryDate);

		tokenRepository.save(newToken);

		// 認証メールを再送信
		mailService.sendVerificationEmail(user.getEmail(), token);
	}
}
