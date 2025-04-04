import { fetchStockData, fetchSMAData } from './stock-api.js';//chart.jsに使うデータをとってくる
import stockConfig from './config/stock-config.js';//銘柄に関する変数配置ファイルをimport
import chartStyleConfig from './config/chart-style-config.js';//グラフに関する変数配置ファイルをimport

// グローバル変数：チャートインスタンスを保持しておく
let candleChart = null;
let volumeChart = null;

// チャートの描画処理（ローソク足と出来高チャートの生成）
export const renderCharts = async () => {
	const data = await fetchStockData(); // データ取得
	const SMAResults = await fetchSMAData();

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

	//SMAのデータsetを
	const SMADatasets = SMAResults.map(sma => ({
		type: "line",
		label: `SMA (${sma.timeperiod})`,
		data: sma.values.map(d => ({ x: d.datetime, y: d.sma })),
		borderColor: chartStyleConfig.getSMAColor(sma.timeperiod),
		borderWidth: 2,
		pointRadius: 0,
		fill: false
	}));

	console.log(SMADatasets);

	// チャートが既にあれば破棄してから再生成（再描画時に必要）
	if (candleChart) {
		candleChart.destroy();
	}
	if (volumeChart) {
		volumeChart.destroy();
	}

	// チャートを生成・描画
	candleChart = createCandleChart(labels, candleData, volumeData, SMADatasets);
	volumeChart = createVolumeChart(labels, volumeData);
}

// ローソク足チャートの作成関数
const createCandleChart = (labels, data, volumeData, SMADatasets) => {
	return new Chart(document.getElementById("candlestick-chart").getContext("2d"), {
		type: "candlestick",
		data: {
			labels,
			datasets: [{
				label: "価格",
				data,
				borderColor: { up: "#26a69a", down: "#ef5350" }, // 緑＝上昇、赤＝下落
				backgroundColor: { up: "#26a69a", down: "#ef5350" }
			},
			...SMADatasets
			]
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
							if (context.dataset.type === "line") {
								const item = context.raw;
								const value = Number(item.y);
								const label = context.dataset.label;
								return isNaN(value) ? `${label}: N/A` : `${label}: ${value.toFixed(4)}`;
							}
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
	stockConfig.interval = event.target.value;
	renderCharts();
});

// 本数変更時に outputsize を更新してチャート再描画
document.getElementById("rowSelector").addEventListener("change", (event) => {
	stockConfig.outputsize = event.target.value;
	renderCharts();
});

// ページ読み込み完了後にチャートを初期描画
document.addEventListener("DOMContentLoaded", renderCharts);