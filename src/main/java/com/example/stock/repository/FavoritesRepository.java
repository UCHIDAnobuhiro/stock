package com.example.stock.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.stock.model.Favorites;
import com.example.stock.model.Tickers;
import com.example.stock.model.Users;

public interface FavoritesRepository extends JpaRepository<Favorites, Long> {
	List<Favorites> findByUser(Users user);

	// 根据 user 和 ticker 查找是否已经存在记录
	Optional<Favorites> findByUserAndTicker(Users user, Tickers ticker);

	Favorites save(Favorites favorite);

}