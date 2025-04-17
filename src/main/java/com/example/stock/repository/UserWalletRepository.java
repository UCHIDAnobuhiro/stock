package com.example.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.stock.model.UserWallet;
import com.example.stock.model.Users;

@Repository
public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {

	UserWallet findByUser(Users user);
}
