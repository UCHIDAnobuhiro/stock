import { fetchStockData, fetchSMAData } from './stock-api.js';//chart.jsに使うデータをとってくる
import stockConfig from './config/stock-config.js';//銘柄に関する変数配置ファイルをimport
import chartStyleConfig from './config/chart-style-config.js';//グラフに関する変数配置ファイルをimport
import { trendlineAnnotations, enableTrendlineDrawing } from './trendline.js';　//トレンドラインのファイルを導入
import { calculateBollingerBands } from './bollinger-calc.js';

// グローバル変数：チャートインスタンスを保持しておく
let candleChart = null;
let volumeChart = null;

stockConfig.showAmount = document.getElementById("rowSelector").value; //同時に表示するデータ数 本数selectorのdefault値をとる

// チャートの描画処理（ローソク足と出来高チャートの生成）
export const renderCharts = async () => {
	//既存のトレンドラインを削除
	for (const key in trendlineAnnotations) {
		delete trendlineAnnotations[key];
	}
	const isSmaChecked = document.querySelector('input[value="sma"]').checked;
	const isBbandsChecked = document.querySelector('input[value="bbands"]').checked;
	const isIchimokuChecked = document.querySelector('input[value="ichimoku"]').checked;
	let extra = 0;

	//
	if (isBbandsChecked) {
		const periods = stockConfig.getSMAPeriods(); // e.g., [5, 25, 75]
		extra = periods[1]; // => 25
	}

	if (isIchimokuChecked) {
		const ichi = stockConfig.getIchimokuPeriods(); // {tenkan, kijun, senkouB}
		extra = Math.max(extra, ichi.senkouB + ichi.kijun); // 通常 52 + 26 = 78
	}
	
	const data = await fetchStockData(extra); // データ取得200+25個

	// x軸用のラベル（日付）
	let labels = data.map(d => d.datetime);

	// ローソク足用のデータ構造に整形
	let candleData = data.map(d => ({
		x: d.datetime,
		o: d.open,
		h: d.high,
		l: d.low,
		c: d.close
	}));

	// 出来高チャート用のデータ
	let volumeData = data.map(d => ({
		x: d.datetime,
		y: d.volume
	}));

	let SMADatasets = [];
	let bbandsDatasets = [];
	let ichimokuDatasets=[];
	//SMAのデータsetを
	if (isSmaChecked || isBbandsChecked) {
		const SMAResults = await fetchSMAData(extra);
		SMADatasets = SMAResults.map(sma => ({
			type: "line",
			label: `SMA (${sma.timeperiod})`,
			data: sma.values.map(d => ({ x: d.datetime, y: parseFloat(d.indicators.sma) })),
			borderColor: chartStyleConfig.getSMAColor(sma.timeperiod),
			borderWidth: 2,
			pointRadius: 0,
			fill: false,
			order: 10
		}));

		if (isBbandsChecked) {

			//中央線設定 "1day": [5, 25, 75]の場合は25の線
			const midSMA = SMAResults[1];
			const smaArray = midSMA.values.map(d => ({
				x: d.datetime,
				y: parseFloat(d.indicators.sma)
			}));

			//毎日の終値を取得
			const closePrices = candleData.map(d => d.c);

			//+2σと-2σを計算する。
			const { upperBand, lowerBand } = calculateBollingerBands(
				smaArray,
				closePrices,
				midSMA.timeperiod
			);

			bbandsDatasets = [
				{
					type: "line",
					label: "+2σ",
					data: upperBand,
					borderColor: chartStyleConfig.getBollingerBandColor("upper"),
					borderWidth: 2,
					pointRadius: 0,
					fill: false,
					order: 10,
					borderDash: [5, 5] // 虛線
				},
				{
					type: "line",
					label: `SMA (${midSMA.timeperiod})`,
					data: smaArray,
					borderColor: chartStyleConfig.getBollingerBandMiddleColor(midSMA.timeperiod),
					borderWidth: 2,
					pointRadius: 0,
					order: 10,
					fill: false
				},
				{
					type: "line",
					label: "-2σ",
					data: lowerBand,
					borderColor: chartStyleConfig.getBollingerBandColor("lower"),
					borderWidth: 2,
					pointRadius: 0,
					fill: false,
					order: 10,
					borderDash: [5, 5] // 虛線
				}
			];
			
			bbandsDatasets = bbandsDatasets.map(ds => ({
					...ds,
					data: ds.data.slice(-stockConfig.outputsize)
				}));
			if (!isSmaChecked) {
				SMADatasets = [];
			}
		}
	}
	
	// チャートが既にあれば破棄してから再生成（再描画時に必要）
	if (candleChart) {
		candleChart.destroy();
	}
	if (volumeChart) {
		volumeChart.destroy();
	}

	//計算用の余計なデータを消す
	if(isBbandsChecked||isIchimokuChecked){
	SMADatasets = SMADatasets.map(ds => ({
		...ds,
		data: ds.data.slice(-stockConfig.outputsize)
	}));
	candleData = candleData.slice(-stockConfig.outputsize);
	volumeData = volumeData.slice(-stockConfig.outputsize);
	labels = labels.slice(-stockConfig.outputsize);
	}
	
	// チャートを生成・描画
	candleChart = createCandleChart(labels, candleData, volumeData, SMADatasets, bbandsDatasets);
	volumeChart = createVolumeChart(labels, volumeData);
	setTimeout(() => {
		enableTrendlineDrawing(candleChart);
	}, 100);
}

// ローソク足チャートの作成関数
const createCandleChart = (labels, data, volumeData, SMADatasets, bbandsDatasets) => {
	let tooltipEl = null;
	let shouldHideTooltip = false; // 表示/非表示を制御するフラグ

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
			...SMADatasets,
			...bbandsDatasets
			]
		},
		options: {
			responsive: true,
			maintainAspectRatio: false,
			animation: {
				duration: 0
			},
			scales: {
				x: {
					type: "category",
					labels: labels,
					display: true, //表示しますが透明化にする

					//いくつのデータを最初に表示する設定
					min: stockConfig.outputsize - chartStyleConfig.showAmount,
					max: stockConfig.outputsize - 1,
					ticks: {
						color: 'rgba(0,0,0,0)',//x軸のずれがないように、x軸を保留し透明化することで表示させない
						maxRotation: 0,
						autoSkipPadding: chartStyleConfig.ticksSkipPadding,//autoSkip:trueなら二つの表のskipされるタブは違う（理由不明）
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
					enabled: false,
					external: function(context) {
						const { chart, tooltip } = context;

						//mouseleave後再度externalを引用される防止
						if (shouldHideTooltip) {
							shouldHideTooltip = false;
							return;
						}

						//tooltipの作成
						if (!tooltipEl && !shouldHideTooltip) {
							tooltipEl = document.createElement('div');
							tooltipEl.id = 'custom-tooltip';
							tooltipEl.style.position = 'absolute';
							tooltipEl.style.pointerEvents = 'none';
							tooltipEl.style.background = 'rgba(0, 0, 0, 0.4)';
							tooltipEl.style.borderRadius = '6px';
							tooltipEl.style.padding = '8px 10px';
							tooltipEl.style.fontFamily = 'sans-serif';
							tooltipEl.style.fontSize = '13px';
							tooltipEl.style.color = '#fff';
							tooltipEl.style.boxShadow = '0 2px 6px rgba(0,0,0,0.25)';
							tooltipEl.style.whiteSpace = 'nowrap';
							tooltipEl.style.zIndex = 999;
							tooltipEl.style.opacity = '1';
							document.body.appendChild(tooltipEl);
						}
						// コンテンツ描画
						const tooltipItems = tooltip.dataPoints;
						const title = tooltip.title?.[0] ?? '';
						let html = `<div style="margin-bottom: 6px; font-weight: bold;">${title}</div>`;

						tooltipItems.forEach((ctx) => {
							const item = ctx.raw;
							const dataset = ctx.dataset;
							const color = dataset.borderColor || '#fff';

							if (item && item.o !== undefined && item.h !== undefined && item.l !== undefined && item.c !== undefined) {
								const volume = volumeData.find(v => v.x === item.x)?.y?.toLocaleString() ?? 'N/A';
								html += `
									<div style="display: flex; align-items: center; margin-bottom: 2px;">
									<span style="width:10px;height:10px;background:${color.up || '#fff'};display:inline-block;margin-right:6px;border-radius:2px;"></span>
									<span>始値: ${item.o.toFixed(4)}</span>
									</div>
									<div style="margin-left:16px;">高値: ${item.h.toFixed(4)}</div>
									<div style="margin-left:16px;">安値: ${item.l.toFixed(4)}</div>
									<div style="margin-left:16px;">終値: ${item.c.toFixed(4)}</div>
									<div style="margin-left:16px;">出来高: ${volume}</div>`;
							} else if (item && item.y !== undefined) {
								const value = Number(item.y);
								html += `
									<div style="display: flex; align-items: center; margin-bottom: 2px;">
									<span style="width:10px;height:10px;background:${color};display:inline-block;margin-right:6px;border-radius:2px;"></span>
									<span>${dataset.label}: ${isNaN(value) ? 'N/A' : value.toFixed(4)}</span></div>`;
							}
						});

						//htmlにtooltipを追加
						tooltipEl.innerHTML = html;

						//左上に配置する
						const canvasRect = chart.canvas.getBoundingClientRect();
						const { chartArea } = chart;
						tooltipEl.style.left = canvasRect.left + 'px';
						tooltipEl.style.top = (canvasRect.top + chartArea.top) + 'px';
						tooltipEl.style.opacity = '1';

						// mouseleaveされたらtooltipを削除
						if (!tooltipEl.dataset.listenerAdded) {
							chart.canvas.addEventListener('mouseleave', () => {
								shouldHideTooltip = true;
								if (tooltipEl) {
									tooltipEl.remove();
									tooltipEl = null;
								}
							});
							tooltipEl.dataset.listenerAdded = 'true';
						}
					}
				},
				annotation: {
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
						onPan: ({ chart }) => { syncChangeScale(chart, chart === candleChart ? volumeChart : candleChart); },
					},
					zoom: {
						enable: false,
					},
					limits: {
						x: {
							minRange: 5,
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
					min: stockConfig.outputsize - chartStyleConfig.showAmount,
					max: stockConfig.outputsize - 1,
					ticks: {
						maxRotation: 0,
						autoSkipPadding: chartStyleConfig.ticksSkipPadding,
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
						onPan: ({ chart }) => { syncChangeScale(chart, chart === candleChart ? volumeChart : candleChart); },
					},
					zoom: {
						enable: false,
					},
					limits: {
						x: {
							minRange: 5,
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

function hideTooltip(tooltipEl) {
	if (tooltipEl) {
		tooltipEl.style.opacity = 0;
		tooltipEl.style.left = '-9999px';
		tooltipEl.style.top = '-9999px';
	}
}

// セレクタ変更時に interval を更新してチャート再描画
document.getElementById("candleSelector").addEventListener("change", (event) => {
	stockConfig.interval = event.target.value;
	renderCharts();
});

// 本数変更時に showAmount を更新してチャート再描画
document.getElementById("rowSelector").addEventListener("change", (event) => {
	chartStyleConfig.showAmount = event.target.value;
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