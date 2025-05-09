/**
 * すでに選択済みのSMAを使ってボリンジャーバンドを計算する
 * @param {Array} smaArray - 中軸となるSMA値配列 [{ x, y }]
 * @param {Array} closePrices - 終値配列 [number]（smaArrayと同じ順）
 * @param {number} period - 期間
 * @param {number} multiplier - 標準偏差倍率（通常2）
 * @returns {Object} { upperBand: [], lowerBand: [] }
 */
export function calculateBollingerBands(smaArray, closePrices, period, multiplier = 2) {
	const upperBand = [];
	const lowerBand = [];

	for (let i = 0; i < smaArray.length; i++) {
		if (i < period - 1 || !smaArray[i].y) {
			upperBand.push({ x: smaArray[i].x, y: null });
			lowerBand.push({ x: smaArray[i].x, y: null });
			continue;
		}

		const window = closePrices.slice(i - period + 1, i + 1);
		const avg = smaArray[i].y;
		const stdDev = Math.sqrt(
			window.reduce((sum, val) => sum + Math.pow(val - avg, 2), 0) / period
		);

		upperBand.push({ x: smaArray[i].x, y: avg + multiplier * stdDev });
		lowerBand.push({ x: smaArray[i].x, y: avg - multiplier * stdDev });
	}

	return { upperBand, lowerBand };
}
