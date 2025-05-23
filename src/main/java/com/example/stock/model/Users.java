package com.example.stock.model;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", schema = "stock")
@Getter
@Setter
@NoArgsConstructor
public class Users implements UserDetails {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotEmpty(message = "名前を入力してください")
	@Size(max = 50, message = "名前は50文字以内で入力してください")
	@Column(nullable = false, length = 50)
	private String username;

	@NotEmpty(message = "メールアドレスを入力してください")
	@Email(message = "正しいメールアドレス形式を入力してください")
	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@NotEmpty(message = "パスワードを入力してください")
	@Size(min = 6, message = "パスワードは6文字以上で入力してください")
	private String password;

	@Column(name = "create_at", nullable = false, updatable = false)
	private LocalDateTime createAt;

	@Column(name = "update_at", nullable = false)
	private LocalDateTime updateAt;

	// @NotEmpty(message = "確認用パスワードを入力してください")
	@Transient
	private String confirmPassword; // データベースに保存しない

	private boolean enabled = false;

	// 連続ログイン失敗回数
	@Column(name = "failed_login_attempts", nullable = false)
	private int failedLoginAttempts = 0;

	// アカウントがロックされているかどうか
	@Column(name = "account_locked", nullable = false)
	private boolean accountLocked = false;

	// ロックされた時間（nullならロックされてない）
	@Column(name = "lock_time")
	private LocalDateTime lockTime;

	// 権限情報（簡単に空リストを返す）
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of();
	}

	@Override
	public String getUsername() {
		return email; // Spring Security のユーザー名としてメールアドレスを使用
	}

	@Override
	public String getPassword() {
		return password;
	}

	public String getDisplayName() {
		return this.username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Users users = (Users) o;
		return id != null && id.equals(users.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

}
