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
		Users user = securityUtils.getLoggedInUserOrThrow();
		Tickers ticker = tickersService.getTickerById(dto.getTickerId());

		String symbol = ticker.getTicker();
		StockCandleWithPrevCloseDto latest = stockService.getLatestStockWithPrevClose(symbol);
		BigDecimal latestClose = BigDecimal.valueOf(latest.getClose());
		System.out.println(latestClose);
		BigDecimal resolvedUnitPrice = resolveUnitPrice(dto, latestClose);
		System.out.println(resolvedUnitPrice);

		trade.setUser(user);
		trade.setTicker(ticker);
		trade.setQuantity(dto.getQuantity());
		trade.setUnitPrice(resolvedUnitPrice);
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

	private BigDecimal calculateTotalPrice(TradeRequestDto dto, BigDecimal resolvedUnitPrice) {
		BigDecimal quantity = dto.getQuantity();
		BigDecimal rate = dto.getExchangeRate();
		String currency = "USD";
		String settlementCurrency = dto.getSettlementCurrency();

		// 1. 如果币种一致，汇率为 1
		if (currency.equalsIgnoreCase(settlementCurrency)) {
			rate = BigDecimal.ONE;
		}

		// 2. 计算原始总价（可保留内部记录）
		BigDecimal total = quantity.multiply(resolvedUnitPrice).multiply(rate);

		// 3. 按照结算币种决定取整策略
		if ("JPY".equalsIgnoreCase(settlementCurrency)) {
			// JPY 向上取整到整数（例如 143682.01 ➝ 143683）
			return total.setScale(0, RoundingMode.CEILING);
		} else {
			// USD 等币种保留两位小数
			return total.setScale(2, RoundingMode.HALF_UP);
		}
	}

	private BigDecimal resolveUnitPrice(TradeRequestDto dto, BigDecimal latestClose) {
		if ("MARKET".equalsIgnoreCase(dto.getType())) {
			if ("buy".equalsIgnoreCase(dto.getSide())) {
				return latestClose.multiply(BigDecimal.valueOf(1.1)).setScale(2, RoundingMode.HALF_UP);
			} else {
				return latestClose.multiply(BigDecimal.valueOf(0.9)).setScale(2, RoundingMode.HALF_UP);
			}
		} else {
			// LIMIT 类型直接取前端输入
			return dto.getUnitPrice();
		}
	}

}
