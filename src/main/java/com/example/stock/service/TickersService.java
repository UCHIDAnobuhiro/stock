package com.example.stock.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.example.stock.exception.TickersException;
import com.example.stock.model.Favorites;
import com.example.stock.model.Tickers;
import com.example.stock.model.Users;
import com.example.stock.repository.FavoritesRepository;
import com.example.stock.repository.TickersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TickersService {

	private final TickersRepository tickersRepository;
	private final FavoritesRepository favoritesRepository;

	/**
	 *  すべてのtickersを取得
	 * 
	 * @return すべてのtickersを含む List<Tickers>。
	 */
	public List<Tickers> getAllTickers() throws TickersException {
		try {
			return tickersRepository.findAll();
		} catch (DataAccessException e) {
			//DataAccessExceptionが発生した場合、エラーメッセージとともにTickersExceptionをスローする
			String errorMessage = "すべての銘柄リスト取得失敗しました: " + e.getMessage();
			throw new TickersException("getAllTickersError", errorMessage);
		}
	}

	public Tickers getTickerById(Long tickerId) {
		Optional<Tickers> optionalTicker = tickersRepository.findById(tickerId);
		Tickers ticker = optionalTicker.orElseThrow(() -> new RuntimeException("Ticker not found"));
		return ticker;
	}

	public List<Tickers> getFavoriteTickersByUser(Users user) throws TickersException {
		List<Favorites> favorites = favoritesRepository.findByUser(user);
		return convertFavoritesToTickers(favorites);
	}

	private List<Tickers> convertFavoritesToTickers(List<Favorites> favorites) {
		List<Tickers> tickersList = new ArrayList<>();
		for (Favorites favorite : favorites) {
			tickersList.add(favorite.getTicker());
		}
		return tickersList;
	}
}
