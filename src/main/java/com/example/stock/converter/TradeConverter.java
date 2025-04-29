package com.example.stock.converter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.stock.dto.StockCandleWithPrevCloseDto;
import com.example.stock.dto.TradeRequestDto;
import com.example.stock.model.Tickers;
import com.example.stock.model.Trade;
import com.example.stock.model.Users;
import com.example.stock.security.SecurityUtils;
import com.example.stock.service.StockService;
import com.example.stock.service.TickersService;

import lombok.AllArgsConstructor;

/**
 * TradeRequestDto を Trade エンティティに変換するためのコンバータークラス。
 * ログイン中のユーザー情報は SecurityUtils から取得される。
 */
@Component
@AllArgsConstructor
public class TradeConverter {

	private final SecurityUtils securityUtils;
	private final TickersService tickersService;
	private final StockService stockService;

	private static final Map<String, Integer> ORDER_TYPE_MAP = Map.of(
			"buy", 0,
			"sell", 1);

	private static final Map<String, Integer> PRICE_TYPE_MAP = Map.of(
			"LIMIT", 0,
			"MARKET", 1);

	/**
	 * フロントエンドからの注文リクエスト DTO を Trade エンティティに変換します。
	 *
	 * @param dto    注文情報（数量、価格、通貨など）
	 * @param ticker 対象の銘柄エンティティ（事前に取得して渡す）
	 * @return 生成された Trade エンティティ（DB保存可能な形式）
	 */
	public Trade toTradeEntity(TradeRequestDto dto) {

		Trade trade = new Trade();

		//必須情報を取得
		Users user = securityUtils.getLoggedInUserOrThrow();
		Tickers ticker = tickersService.getTickerById(dto.getTickerId());

		//現在値を取得
		String symbol = ticker.getTicker();
		StockCandleWithPrevCloseDto latest = stockService.getLatestStockWithPrevClose(symbol);
		BigDecimal latestClose = BigDecimal.valueOf(latest.getClose());
		BigDecimal resolvedUnitPrice = resolveUnitPrice(dto, latestClose);//成行の値段計算±10％

		trade.setUser(user);
		trade.setTicker(ticker);
		trade.setQuantity(dto.getQuantity());
		trade.setUnitPrice(resolvedUnitPrice);

		//総受渡金額を計算し保存
		trade.setTotalPrice(calculateTotalPrice(dto, resolvedUnitPrice));
		trade.setCurrency("USD");
		trade.setSettlementCurrency(dto.getSettlementCurrency());
		trade.setExchangeRate(dto.getExchangeRate());
		trade.setSide(ORDER_TYPE_MAP.get(dto.getSide()));
		trade.setType(PRICE_TYPE_MAP.get(dto.getType()));
		trade.setStatus(4); // デフォルトは完成
		trade.setCreateAt(LocalDateTime.now());
		trade.setUpdateAt(LocalDateTime.now());

		return trade;
	}

	//総受渡金額を計算する
	private BigDecimal calculateTotalPrice(TradeRequestDto dto, BigDecimal resolvedUnitPrice) {
		BigDecimal quantity = dto.getQuantity();

		//thymeleafで手動で設定した為替レート
		BigDecimal rate = dto.getExchangeRate();

		//基準となる通貨をusdに設定（米国株のため）
		String currency = "USD";

		//決済通貨を取得
		String settlementCurrency = dto.getSettlementCurrency();

		// 基準通貨と決済通貨一緒なら為替レートを1に設定
		if (currency.equalsIgnoreCase(settlementCurrency)) {
			rate = BigDecimal.ONE;
		}

		//総受渡金額の計算
		BigDecimal total = quantity.multiply(resolvedUnitPrice).multiply(rate);

		// 通貨による総受渡金額の桁数処理
		if ("JPY".equalsIgnoreCase(settlementCurrency)) {
			// JPY 切り上げ（例えば：2.01 ➝ 3）
			return total.setScale(0, RoundingMode.CEILING);
		} else {
			// USD 小数点以下2桁を保留　四捨五入
			return total.setScale(2, RoundingMode.HALF_UP);
		}
	}

	private BigDecimal resolveUnitPrice(TradeRequestDto dto, BigDecimal latestClose) {

		//成行の場合は現在値を±10％し、四捨五入し、小数点以下2桁残す。
		if ("MARKET".equalsIgnoreCase(dto.getType())) {
			if ("buy".equalsIgnoreCase(dto.getSide())) {
				return latestClose.multiply(BigDecimal.valueOf(1.1)).setScale(2, RoundingMode.HALF_UP);
			} else {
				return latestClose.multiply(BigDecimal.valueOf(0.9)).setScale(2, RoundingMode.HALF_UP);
			}
		} else {

			//指値なら処理いらない
			return dto.getUnitPrice();
		}
	}

}
