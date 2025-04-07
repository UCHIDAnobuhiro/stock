package com.example.stock.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.stock.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
	private final UsersRepository usersRepository;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		// 認証設定
		http.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/css/**", "/js/**", "/images/**").permitAll() // 静的リソースは常に許可
						.requestMatchers("/", "/login", "/signup", "/verify/**", "/password/**")
						.permitAll() // 全ユーザーに開放するUR
						.requestMatchers("/admin/**").hasRole("ADMIN") // 管理者ロールにのみ許可
						.anyRequest().authenticated() // 他のエンドポイントは認証が必要
				);
		// ログインフォームの設定
		http.formLogin(login -> login
				.loginPage("/login") // カスタムログインページ
				.usernameParameter("email") // フォームの `name="email"` に対応
				.failureHandler(customLoginFailureHandler()) // 認証失敗時のハンドラーを適用
				.successHandler(customLoginSuccessHandler()) // 認証成功時のハンドラーを適用
				.permitAll());

		// ログアウトの設定
		http.logout(logout -> logout
				.logoutUrl("/logout")
				.logoutSuccessUrl("/login")
				.permitAll());

		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(); // BCryptでハッシュ化
	}

	@Bean
	CustomLoginSuccessHandler customLoginSuccessHandler() {
		return new CustomLoginSuccessHandler(usersRepository);
	}

	@Bean
	CustomLoginFailureHandler customLoginFailureHandler() {
		return new CustomLoginFailureHandler(usersRepository);
	}

}