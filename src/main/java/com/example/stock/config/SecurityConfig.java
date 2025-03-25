package com.example.stock.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		// 認証設定
		http.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/css/**", "/js/**", "/images/**").permitAll() // 静的リソースは常に許可
						.requestMatchers("/", "/login", "/signup").permitAll() // 全ユーザーに開放するUR
						.requestMatchers("/admin/**").hasRole("ADMIN") // 管理者ロールにのみ許可
						.requestMatchers("/worktime/**").authenticated() // `worktime` を認証必須にする
						.anyRequest().authenticated() // 他のエンドポイントは認証が必要
				);
		// ログインフォームの設定
		http.formLogin(login -> login
				.loginPage("/login") // カスタムログインページ
				.usernameParameter("mail") // フォームの `name="mail"` に対応
				.defaultSuccessUrl("/stock", true) // ログイン成功後の遷移先
				.failureHandler(authenticationFailureHandler()) // 認証失敗時のハンドラーを適用
				.permitAll());

		// ログアウトの設定
		http.logout(logout -> logout
				.logoutUrl("/logout")
				.logoutSuccessUrl("/login")
				.permitAll());

		return http.build();
	}

	@Bean
	AuthenticationFailureHandler authenticationFailureHandler() {
		return new SimpleUrlAuthenticationFailureHandler("/login?error=true");
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(); // BCryptでハッシュ化
	}

}