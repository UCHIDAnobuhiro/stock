export function calculateIchimoku(candleData, { tenkanPeriod, kijunPeriod, senkouBPeriod,chikouOffsetPeriod}) {
	const ichimoku = {
		tenkan: [],
		kijun: [],
		senkouA: [],
		senkouB: [],
		chikou: []
	};

	for (let i = 0; i < candleData.length; i++) {
		const getHighLow = (period) => {
			const slice = candleData.slice(Math.max(0, i - period + 1), i + 1);
			const highs = slice.map(d => d.h);
			const lows = slice.map(d => d.l);
			return { high: Math.max(...highs), low: Math.min(...lows) };
		};

		const { high: highT, low: lowT } = getHighLow(tenkanPeriod);
		const { high: highK, low: lowK } = getHighLow(kijunPeriod);
		const { high: highB, low: lowB } = getHighLow(senkouBPeriod);

		const tenkan = (highT + lowT) / 2;
		const kijun = (highK + lowK) / 2;
		const senkouA = (tenkan + kijun) / 2;
		const senkouB = (highB + lowB) / 2;

		if (i >= chikouOffsetPeriod && i < candleData.length) {
			ichimoku.chikou.push({
				x: candleData[i - chikouOffsetPeriod].x,
				y: candleData[i].c
			});
		}

		ichimoku.tenkan.push({ x: candleData[i].x, y: tenkan });
		ichimoku.kijun.push({ x: candleData[i].x, y: kijun });

		if (i + kijunPeriod < candleData.length) {
			ichimoku.senkouA.push({ x: candleData[i + kijunPeriod].x, y: senkouA });
			ichimoku.senkouB.push({ x: candleData[i + kijunPeriod].x, y: senkouB });
		}
	}

	return ichimoku;
}
