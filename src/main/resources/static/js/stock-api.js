import stockConfig from './config/stock-config.js';

/**
 * 株価のローソク足データ（時系列）を取得する非同期関数。
 *
 * 現在の設定（symbol, interval, outputsize）に基づいてAPIからデータを取得し、
 * 時系列を昇順に並べ替えた配列を返します。
 *
 * @returns {Promise<Object[]>} ローソク足データの配列（datetime, open, high, low, close, volume を含む）
 *
 * @example
 * const data = await fetchStockData();
 * console.log(data[0].close); // 終値を出力
 */
export const fetchStockData = async (extra=0) => {
	const actualOutputsize = stockConfig.outputsize + extra;
	const url = `/api/stocks/list?symbol=${stockConfig.symbol}&interval=${stockConfig.interval}&outputsize=${actualOutputsize}`;
	const res = await fetch(url);
	const json = await res.json();

	if (json.status === "error") {
		console.error("Stock API error:", json.message);
		return [];
	}
	return json.reverse(); // 昇順で返す
};


/**
 * 指定された interval に応じた複数の SMA（単純移動平均）データを取得する。
 *
 * stockConfig の設定に従い、対応する複数の期間（例: [5, 25, 75]）ごとにデータを取得し、
 * それぞれの期間ごとの配列をまとめて返す。
 *
 * @returns {Promise<Object[]>} SMAデータの配列（各要素は { timeperiod, values: [{ datetime, sma }] }）
 *
 * @example
 * const smaList = await fetchSMAData();
 * console.log(smaList[0].timeperiod); // 例: 5
 * console.log(smaList[0].values[0].sma); // 例: 220.12
 */
export const fetchSMAData = async (extra = 0,dataLength) => {
	const interval = stockConfig.interval;
	const timePeriods = stockConfig.getSMAPeriods();
	let actualOutputsize = stockConfig.outputsize + extra;
	
	const fetchOne = async (period) => {
		
		if (period > dataLength) {
			console.warn(`Skipping SMA(${period}) — Not enough candle data (${dataLength})`);
			return null;
		}

		//ロウソク足データが少ない場合、それに合わせてsmaも少なめにfetchする。
		if (dataLength < stockConfig.outputsize) {
			actualOutputsize =extra+ dataLength - period + 1;
		}
		
		const url = `/api/stocks/technical/SMA?symbol=${stockConfig.symbol}&interval=${interval}&timeperiod=${period}&outputsize=${actualOutputsize}`;
		console.log(url);
		const res = await fetch(url);
		const json = await res.json();
		if (json.status === "error") {
			console.error(`SMA(${period}) API error:`, json.message);
			return null;
		}
		return {
			timeperiod: period,
			values: json
		};
	};

	const results = await Promise.all(timePeriods.map(fetchOne));
	return results.filter(Boolean);
};
