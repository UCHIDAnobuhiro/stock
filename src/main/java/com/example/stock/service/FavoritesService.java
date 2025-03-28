package com.example.stock.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.stock.model.Favorites;
import com.example.stock.model.Tickers;
import com.example.stock.model.Users;
import com.example.stock.repository.FavoritesRepository;

@Service
public class FavoritesService {

	@Autowired
	private FavoritesRepository favoritesRepository;

	// 根据用户和ticker创建或更新喜欢记录
	public Favorites addFavorite(Users user, Tickers ticker) {
		// 查找是否已经存在喜欢记录
		Optional<Favorites> existingFavorite = favoritesRepository.findByUserAndTicker(user, ticker);

		if (existingFavorite.isPresent()) {
			// 如果已存在喜欢记录，返回该记录
			return existingFavorite.get();
		}

		// 如果不存在，则创建新的喜欢记录
		Favorites favorite = new Favorites();
		favorite.setUser(user);
		favorite.setTicker(ticker);

		//nowは例：2025-03-28 16:45:32.257510
		LocalDateTime now = LocalDateTime.now();
		//秒までに変形
		LocalDateTime truncatedNow = now.truncatedTo(ChronoUnit.SECONDS);
		favorite.setCreateAt(truncatedNow);
		return favoritesRepository.save(favorite);
	}

	public void deleteFavorite(Users user, Tickers ticker) {
		Optional<Favorites> existingFavorite = favoritesRepository.findByUserAndTicker(user, ticker);
		existingFavorite.ifPresent(favoritesRepository::delete);
	}
}
