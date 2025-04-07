package com.example.stock.service;

import java.time.LocalDateTime;

import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.stock.model.Users;
import com.example.stock.repository.UsersRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {
	private final UsersRepository usersRepository;

	public CustomUserDetailsService(UsersRepository usersRepository) {
		this.usersRepository = usersRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Users user = usersRepository.findByEmail(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

		// ロックされていたらチェック
		if (user.isAccountLocked()) {
			if (user.getLockTime() != null &&
					user.getLockTime().plusMinutes(5).isBefore(LocalDateTime.now())) {
				// ロック解除
				user.setAccountLocked(false);
				user.setFailedLoginAttempts(0);
				user.setLockTime(null);
				usersRepository.save(user);
			} else {
				throw new LockedException("アカウントがロックされています。5分後に再試行してください。");
			}
		}

		return user;
	}
}
