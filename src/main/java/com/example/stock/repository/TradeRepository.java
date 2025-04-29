package com.example.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.stock.model.Trade;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

}
