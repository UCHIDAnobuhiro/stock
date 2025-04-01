package com.example.stock.converter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.stock.dto.TickersWithFavoriteDTO;
import com.example.stock.model.Favorites;
import com.example.stock.model.Tickers;

public class TickersDTOConverter {

	public static List<TickersWithFavoriteDTO> convertToTickersWithFavoriteDTO(List<Tickers> tickers,
			List<Favorites> favorites) {
		//setすることで、DTOに値えを設定するのはやりやすい
		Set<Long> favoriteTickerIds = favorites.stream()
				.map(fav -> fav.getTicker().getId())
				.collect(Collectors.toSet());

		//  DTOを組み合わせ
		return tickers.stream()
				.map(ticker -> new TickersWithFavoriteDTO(

						//tickersのデータをコピー
						ticker.getId(),
						ticker.getTicker(),
						ticker.getBrand(),

						//favoritesに該当するtickeridがあれば追加する
						favoriteTickerIds.contains(ticker.getId())))
				.collect(Collectors.toList());
	}
}
