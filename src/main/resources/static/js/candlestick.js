// 初期設定：表示する銘柄とローソク足の時間軸
let symbol = 'AAPL';
let interval = '1day';
let outputsize = 100;

// グローバル変数：チャートインスタンスを保持しておく
let candleChart = null;
let volumeChart = null;

export const setSymbol = (newSymbol) => {
	symbol = newSymbol;
};

export const getSymbol = () => {
	return symbol;
};

// 株価データをAPIから取得する非同期関数
const fetchStockData = async () => {
	const url = `/api/stocks/time-series/values?symbol=${symbol}&interval=${interval}&outputsize=${outputsize}`;
	const res = await fetch(url);
	const json = await res.json();

	// APIエラーがあればログに出力して中断
	if (json.status === "error") {
		console.error("API error:", json.message);
		return;
	}

	// データは時系列の降順で返ってくる想定 → 昇順に直す
	const rawData = json.reverse();
	return rawData;
}

// チャートの描画処理（ローソク足と出来高チャートの生成）
export const renderCharts = async () => {
	const data = await fetchStockData(); // データ取得

	// x軸用のラベル（日付）
	const labels = data.map(d => d.datetime);

	// ローソク足用のデータ構造に整形
	const candleData = data.map(d => ({
		x: d.datetime,
		o: d.open,
		h: d.high,
		l: d.low,
		c: d.close
	}));

	// 出来高チャート用のデータ
	const volumeData = data.map(d => ({
		x: d.datetime,
		y: d.volume
	}));

	// チャートが既にあれば破棄してから再生成（再描画時に必要）
	if (candleChart) {
		candleChart.destroy();
	}
	if (volumeChart) {
		volumeChart.destroy();
	}

	// チャートを生成・描画
	candleChart = createCandleChart(labels, candleData, volumeData);
	volumeChart = createVolumeChart(labels, volumeData);
}

// ローソク足チャートの作成関数
const createCandleChart = (labels, data, volumeData) => {
	return new Chart(document.getElementById("candlestick-chart").getContext("2d"), {
		type: "candlestick",
		data: {
			labels,
			datasets: [{
				label: "価格",
				data,
				borderColor: { up: "#26a69a", down: "#ef5350" }, // 緑＝上昇、赤＝下落
				backgroundColor: { up: "#26a69a", down: "#ef5350" }
			}]
		},
		options: {
			responsive: true,
			maintainAspectRatio: false,
			scales: {
				x: {
					type: "category",
					labels: labels,
					ticks: {
						maxRotation: 0,
						autoSkip: true,
						maxTicksLimit: 10,
						callback: (val, index) => labels[index]
					},
				},
				y: {
					position: "right",
					ticks: {
						callback: (value) => value.toFixed(2)// 小数2桁で表示
					},
					afterFit: scale => {
						scale.width = 90; // Y軸幅を固定
					}
				}
			},
			plugins: {
				tooltip: {
					callbacks: {
						// ツールチップタイトル（日付）
						title: (context) => context[0].label,
						// ツールチップ内容（OHLC + 出来高）
						label: (context) => {
							const item = context.raw;
							const matchedVolume = volumeData.find(v => v.x === item.x);
							const volume = matchedVolume ? matchedVolume.y.toLocaleString() : "N/A";
							return [
								`始値: ${item.o.toFixed(4)}`,
								`高値: ${item.h.toFixed(4)}`,
								`安値: ${item.l.toFixed(4)}`,
								`終値: ${item.c.toFixed(4)}`,
								`出来高: ${volume}`
							];
						}
					}
				},
				legend: { display: false } // 凡例は非表示
			}
		}
	});
}

// 出来高チャートの作成関数
const createVolumeChart = (labels, data) => {
	return new Chart(document.getElementById("volume-chart").getContext("2d"), {
		type: "bar",
		data: {
			labels,
			datasets: [{
				label: "出来高",
				data,
				backgroundColor: "rgba(100, 149, 237, 0.4)", // 柔らかい青色
				borderColor: "rgba(100, 149, 237, 1)",
				barThickness: 5 // 棒の太さ
			}]
		},
		options: {
			responsive: true,
			maintainAspectRatio: false,
			scales: {
				x: {
					type: "category",
					labels: labels,
					ticks: {
						maxRotation: 0,
						autoSkip: true,
						maxTicksLimit: 10,
						callback: (val, index) => labels[index]
					},
					grid: {
						display: false // x軸のグリッド非表示
					}
				},
				y: {
					position: "right",
					ticks: {
						// 1000単位で"K"表示
						callback: v => v === 0 ? "0" : `${(v / 1_000).toLocaleString()}K`
					},
					afterFit: scale => {
						scale.width = 90;
					}
				}
			},
			plugins: {
				tooltip: {
					callbacks: {
						title: ctx => ctx[0].label,
						label: ctx => `出来高: ${ctx.raw.y.toLocaleString()}`
					}
				},
				legend: { display: false }
			}
		}
	});
}

// セレクタ変更時に interval を更新してチャート再描画
document.getElementById("candleSelector").addEventListener("change", (event) => {
	interval = event.target.value;
	renderCharts();
});

// 本数変更時に outputsize を更新してチャート再描画
document.getElementById("rowSelector").addEventListener("change", (event) => {
	outputsize = event.target.value;
	renderCharts();
});

// ページ読み込み完了後にチャートを初期描画
document.addEventListener("DOMContentLoaded", renderCharts);