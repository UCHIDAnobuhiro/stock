package com.example.stock.service;

import org.springframework.stereotype.Service;

import com.example.stock.model.UserWallet;
import com.example.stock.model.Users;
import com.example.stock.repository.UserWalletRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserWalletService {
	private final UserWalletRepository userWalletRepository;

	public UserWallet getWalletByUser(Users user) {
		return userWalletRepository.findByUser(user);
	}

}
