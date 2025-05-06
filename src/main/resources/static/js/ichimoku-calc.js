// Ichimoku（一目均衡表）を計算する関数
export function calculateIchimoku(candleData, { tenkanPeriod, kijunPeriod, senkouBPeriod, chikouOffsetPeriod }) {
	const ichimoku = {
		tenkan: [],   // 転換線
		kijun: [],    // 基準線
		senkouA: [],  // 先行スパンA
		senkouB: [],  // 先行スパンB
		chikou: []    // 遅行スパン
	};

	for (let i = 0; i < candleData.length; i++) {
		// 過去n期間の高値と安値の範囲を取得するヘルパー関数
		const getHighLow = (period) => {
			const slice = candleData.slice(Math.max(0, i - period + 1), i + 1);
			const highs = slice.map(d => d.h); // 高値
			const lows = slice.map(d => d.l);  // 安値
			return { high: Math.max(...highs), low: Math.min(...lows) };
		};

		// 転換線・基準線・先行スパンBの計算に必要な高値/安値の取得
		const { high: highT, low: lowT } = getHighLow(tenkanPeriod);
		const { high: highK, low: lowK } = getHighLow(kijunPeriod);
		const { high: highB, low: lowB } = getHighLow(senkouBPeriod);

		// 各指標の計算
		const tenkan = (highT + lowT) / 2;         // 転換線 = (過去N期間の高値 + 安値) / 2
		const kijun = (highK + lowK) / 2;          // 基準線
		const senkouA = (tenkan + kijun) / 2;      // 先行スパンA = (転換線 + 基準線) / 2
		const senkouB = (highB + lowB) / 2;        // 先行スパンB = (過去N期間の高値 + 安値) / 2

		// 遅行スパンの計算（現在の終値を、過去N期間前の日付にプロット）
		if (i >= chikouOffsetPeriod && i < candleData.length) {
			ichimoku.chikou.push({
				x: candleData[i - chikouOffsetPeriod].x, // 遅らせる
				y: candleData[i].c                        // 現在の終値
			});
		}

		// 転換線・基準線の現在時点のデータ追加
		ichimoku.tenkan.push({ x: candleData[i].x, y: tenkan });
		ichimoku.kijun.push({ x: candleData[i].x, y: kijun });

		// 先行スパンA・BのX軸を未来へずらす処理
		let futureX;
		if (i + chikouOffsetPeriod < candleData.length) {
			// データがある場合は既存のxを使用
			futureX = candleData[i + chikouOffsetPeriod].x;
		} else {
			// データがない場合は未来の日付を生成
			const lastX = candleData[candleData.length - 1].x;
			const extraDays = i + chikouOffsetPeriod - candleData.length;
			futureX = addDays(lastX, extraDays);
		}

		// 先行スパンA・Bに未来位置のデータを追加
		ichimoku.senkouA.push({ x: futureX, y: senkouA });
		ichimoku.senkouB.push({ x: futureX, y: senkouB });
	}

	// --- 遅行スパンの不足xをnullで補完（全期間に合わせる） ---
	const tenkanXs = ichimoku.tenkan.map(d => d.x);
	const chikouXs = new Set(ichimoku.chikou.map(d => d.x));

	for (const x of tenkanXs) {
		if (!chikouXs.has(x)) {
			ichimoku.chikou.push({ x, y: null });
		}
	}

	// --- 先行スパンの期間分だけ、未来の日付を使ってnull値でtenkan/kijun/chikouを補完 ---
	for (let j = 0; j < chikouOffsetPeriod; j++) {
		const x = addDays(candleData[candleData.length - 1].x, j + 1); // 未来の日付を1日ずつ追加
		ichimoku.tenkan.push({ x, y: null });
		ichimoku.kijun.push({ x, y: null });
		ichimoku.chikou.push({ x, y: null });
	}

	return ichimoku;
}

// 指定した日付にn日を加算して、"YYYY-MM-DD"形式で返す
function addDays(dateStr, days) {
	const date = new Date(dateStr);
	date.setDate(date.getDate() + days);
	return date.toISOString().slice(0, 10);
}
