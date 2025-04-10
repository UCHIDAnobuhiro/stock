package com.example.stock.converter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.stock.dto.FlexibleIndicatorDto;
import com.example.stock.model.TechnicalIndicatorValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TechnicalIndicatorConverter {
	private final ObjectMapper objectMapper;

	/**
	 * 柔軟なテクニカル指標DTOリストを、永続化可能なエンティティリストに変換します。
	 * 各DTO内のdatetimeと複数の指標値（Map）を1件ずつ展開し、個別のエンティティとして構築します。
	 *
	 * @param symbol 株式のシンボル（例: AAPL）
	 * @param interval データの間隔（例: 1day, 1min など）
	 * @param indicator 指標の種類（例: SMA, BollingerBands など）
	 * @param period 指標の期間（例: 25, 75など）
	 * @param dtoList パース済みのFlexibleIndicatorDtoのリスト
	 * @return TechnicalIndicatorValueエンティティのリスト
	 */
	public List<TechnicalIndicatorValue> toEntities(
			String symbol, String interval, String indicator, Integer period, List<FlexibleIndicatorDto> dtoList) {

		List<TechnicalIndicatorValue> entities = new ArrayList<>();

		for (FlexibleIndicatorDto dto : dtoList) {
			LocalDateTime datetime = LocalDate.parse(dto.getDatetime()).atStartOfDay();

			for (Map.Entry<String, String> entry : dto.getIndicators().entrySet()) {
				TechnicalIndicatorValue entity = new TechnicalIndicatorValue();
				entity.setSymbol(symbol);
				entity.setInterval(interval);
				entity.setDatetime(datetime);
				entity.setIndicator(indicator);
				entity.setLineName(entry.getKey());
				entity.setPeriod(period);
				entity.setValue(Double.valueOf(entry.getValue()));
				entities.add(entity);
			}
		}

		return entities;
	}

	/**
	 * エンティティのリスト（{@link TechnicalIndicatorValue}）から柔軟な形式のDTOリスト
	 * （{@link FlexibleIndicatorDto}）に変換します。
	 *
	 * 各エンティティは1つの指標名（lineName）とその値（value）を持っており、
	 * 同じ日時（datetime）を持つエンティティ同士は1つのDTOにまとめられます。
	 * 
	 * DTOのdatetimeは "yyyy-MM-dd HH:mm:ss" 形式の文字列に整形され、
	 * 指標名と値のマップ（Map<String, String>）として格納されます。
	 *
	 * 例:  
	 * - 複数の lineName ("sma", "upper" など) を含むローソク足情報を1件のDTOにまとめたい場合に使用。
	 *
	 * @param entities {@link TechnicalIndicatorValue} のリスト（データベースから取得したエンティティ群）
	 * @return {@link FlexibleIndicatorDto} のリスト（指標ごとにグループ化されたDTO群）
	 */
	public static List<FlexibleIndicatorDto> fromEntities(List<TechnicalIndicatorValue> entities) {
		Map<LocalDateTime, Map<String, String>> groupedByDatetime = new TreeMap<>();

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		for (TechnicalIndicatorValue entity : entities) {
			LocalDateTime dt = entity.getDatetime();
			String lineName = entity.getLineName();
			String valueStr = String.valueOf(entity.getValue());

			groupedByDatetime
					.computeIfAbsent(dt, k -> new HashMap<>())
					.put(lineName, valueStr);
		}

		List<FlexibleIndicatorDto> dtoList = new ArrayList<>();
		for (Map.Entry<LocalDateTime, Map<String, String>> entry : groupedByDatetime.entrySet()) {
			FlexibleIndicatorDto dto = new FlexibleIndicatorDto();
			dto.setDatetime(entry.getKey().format(formatter));
			dto.setIndicators(entry.getValue());
			dtoList.add(dto);
		}

		return dtoList;
	}

	/**
	 * Twelve Data APIから取得したJSON文字列を解析し、柔軟なテクニカル指標DTOリストに変換します。
	 * 各データ行にはdatetimeと、動的に変化する1つ以上の指標名（sma、upperなど）を含みます。
	 *
	 * @param json Twelve Data APIから取得したJSON文字列
	 * @return FlexibleIndicatorDtoのリスト（datetime + Map<指標名, 値>）
	 * @throws IOException JSONの解析中にエラーが発生した場合にスローされます
	 */
	public List<FlexibleIndicatorDto> parseDtoFromJson(String json) throws IOException {
		Map<String, Object> responseMap = objectMapper.readValue(json, new TypeReference<>() {
		});
		List<Map<String, String>> rawValues = (List<Map<String, String>>) responseMap.get("values");

		return rawValues.stream().map(raw -> {
			FlexibleIndicatorDto dto = new FlexibleIndicatorDto();
			dto.setDatetime(raw.get("datetime"));

			Map<String, String> indicators = new HashMap<>(raw);
			indicators.remove("datetime");
			dto.setIndicators(indicators);
			return dto;
		}).collect(Collectors.toList());
	}

}
