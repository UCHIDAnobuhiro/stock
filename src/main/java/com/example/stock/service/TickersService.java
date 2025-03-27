package com.example.stock.service;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.example.stock.exception.TickersException;
import com.example.stock.model.Tickers;
import com.example.stock.repository.TickersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TickersService {

	private final TickersRepository tickersRepository;

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

}
