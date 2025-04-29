package com.example.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.stock.model.UserWalletLog;

@Repository
public interface UserWalletLogRepository extends JpaRepository<UserWalletLog, Long> {
}
