import { fetchStockData, fetchSMAData } from './stock-api.js';//chart.jsに使うデータをとってくる
import stockConfig from './config/stock-config.js';//銘柄に関する変数配置ファイルをimport
import chartStyleConfig from './config/chart-style-config.js';//グラフに関する変数配置ファイルをimport
import { trendlineAnnotations, enableTrendlineDrawing } from './trendline.js';　//トレンドラインのファイルを導入


// グローバル変数：チャートインスタンスを保持しておく
let candleChart = null;
let volumeChart = null;

let showAmount= 100;
let adjustSpeed= 20;
let minTicks= 10;
let ticksSkipPadding= 5;
let dataLength;
// チャートの描画処理（ローソク足と出来高チャートの生成）
export const renderCharts = async () => {
	//既存のトレンドラインを削除
	for (const key in trendlineAnnotations) {
		delete trendlineAnnotations[key];
	}
	const isSmaChecked = document.querySelector('input[value="sma"]').checked;
	const data = await fetchStockData(); // データ取得

	//データの長さを更新
	dataLength = data.length;
	
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

	let SMADatasets = [];
	//SMAのデータsetを
	if (isSmaChecked) {
		const SMAResults = await fetchSMAData();
		SMADatasets = SMAResults.map(sma => ({
			type: "line",
			label: `SMA (${sma.timeperiod})`,
			data: sma.values.map(d => ({ x: d.datetime, y: d.sma })),
			borderColor: chartStyleConfig.getSMAColor(sma.timeperiod),
			borderWidth: 2,
			pointRadius: 0,
			fill: false
		}));
	}
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
	setTimeout(() => {
		enableTrendlineDrawing(candleChart);
	}, 100);
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
			animation: false, // 動画を消す
			scales: {
				x: {
					type: "category",
					labels: labels,
					display: true, //表示しますが透明化にする

					//いくつのデータを最初に表示する設定
					min: dataLength - showAmount,//何個を表示するのか
					max: dataLength - 1,//データの最後から表示
					ticks: {
						color: 'rgba(0,0,0,0)',//x軸のずれがないように、x軸を保留し透明化することで表示させない
						maxRotation: 0,
						autoSkipPadding: ticksSkipPadding,//autoSkip:trueなら二つの表のskipされるタブは違う（理由不明）
						callback: (index) => labels[index]
					},
					grid: {
						offset: false,
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
				annotation: {
					draggable: true,
					annotations: trendlineAnnotations, // トレンドラインを追加
					interaction: {
						mode: 'nearest',
						intersect: true,
					},
				},
				zoom: {
					pan: {
						enabled: true,
						mode: 'x',
						speed: adjustSpeed,
						onPan: ({ chart }) => { syncChangeScale(chart, chart === candleChart ? volumeChart : candleChart); },
					},
					zoom: {
						enable: false,
					},
					limits: {
						x: {
							minRange: minTicks,
						},
					},
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
				barThickness: 'flex' // 棒の太さ
			}]
		},
		options: {
			responsive: true,
			maintainAspectRatio: false,
			scales: {
				x: {
					type: "category",
					labels: labels,
					min: dataLength - showAmount,
					max: dataLength - 1,
					ticks: {
						maxRotation: 0,
						autoSkipPadding: ticksSkipPadding,
						callback: (index) => labels[index],
					},
					grid: {
						offset: false,//グリードを棒の中央に設置
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
				zoom: {
					pan: {
						enabled: true,
						mode: 'x',
						speed: adjustSpeed,
						onPan: ({ chart }) => { syncChangeScale(chart, chart === candleChart ? volumeChart : candleChart); },
					},
					zoom: {
						enable: false,
					},
					limits: {
						x: {
							minRange: minTicks,
						},
					},
				},
				legend: { display: false }
			}
		}
	});
}

/**
 * 2つのチャートのX軸の表示範囲を同期します（ズームやパン操作時）。
 * このメソッドは、ソースチャートのX軸の最小値（min）と最大値（max）をターゲットチャートに同期させ、
 * 両方のチャートが同じ時間範囲を表示するようにします。
 * ズームやパン操作時に複数のチャートの表示範囲を一致させる場合に使用します。
 * 
 * @param {Chart} sourceChart - ズームまたはパン操作を行うソースチャート（通常は操作中のチャート）。
 * @param {Chart} targetChart - 表示範囲を同期させるターゲットチャート（通常は別のチャート）。
 * 
 * @example
 * // ろうそく足チャートと出来高チャートのズーム範囲を同期させる
 * syncChangeScale(candleChart, volumeChart);
 */
const syncChangeScale = (sourceChart, targetChart) => {
	if (!sourceChart || !targetChart) return; // null check

	//現在操作中のChartのXの左と右lableを取得（表示されている範囲）
	const newScale = sourceChart.scales.x;

	// すでに同じscaleなら更新しない
	if (
		targetChart.options.scales.x.min !== newScale.min ||
		targetChart.options.scales.x.max !== newScale.max
	) {
		//目標chartのｘ軸の表示範囲を同期
		targetChart.options.scales.x.min = newScale.min;
		targetChart.options.scales.x.max = newScale.max;
		targetChart.update("none"); // アニメーション更新はしない、データだけの更新をする
	}
};


// セレクタ変更時に interval を更新してチャート再描画
document.getElementById("candleSelector").addEventListener("change", (event) => {
	stockConfig.interval = event.target.value;
	renderCharts();
});

// 本数変更時に showAmount を更新してチャート再描画
document.getElementById("rowSelector").addEventListener("change", (event) => {
	stockConfig.outputsize = event.target.value;
	renderCharts();
});

// テクニカルのチェック状態が変わったら再描画
document.querySelectorAll('#technicalDropdownMenu input[type="checkbox"]').forEach(event => {
	event.addEventListener("change", () => {
		renderCharts();
	});
});

// ページ読み込み完了後にチャートを初期描画
document.addEventListener("DOMContentLoaded", renderCharts);