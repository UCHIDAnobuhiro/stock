import { fetchStockData, fetchSMAData } from './stock-api.js'; // chart.jsに使うデータをとってくる
import stockConfig from './config/stock-config.js'; // 銘柄に関する変数配置ファイルをimport
import chartStyleConfig from './config/chart-style-config.js'; // グラフに関する変数配置ファイルをimport
import { trendlineAnnotations, enableTrendlineDrawing } from './trendline.js'; // トレンドラインのファイルを導入
import { calculateBollingerBands } from './bollinger-calc.js';
import { calculateIchimoku } from './ichimoku-calc.js';

//ロゴ画面から遷移時にsymbolを更新してから、chartを描く
document.addEventListener("DOMContentLoaded", () => {
	stockConfig.initFromDOM();  // DOM から symbol を取得
});

// グローバル変数：チャートインスタンスを保持しておく
let candleChart = null;
let volumeChart = null;
let labels;

// デフォルトの表示本数をセレクターから取得
stockConfig.showAmount = document.getElementById("rowSelector").value;

// チャートの描画処理（ローソク足と出来高チャートの生成）
export const renderCharts = async () => {
	// 既存のトレンドラインを削除
	for (const key in trendlineAnnotations) {
		delete trendlineAnnotations[key];
	}

	// チェックされたインジケーターの確認
	const isSmaChecked = document.querySelector('input[value="sma"]').checked;
	const isBbandsChecked = document.querySelector('input[value="bbands"]').checked;
	const isIchimokuChecked = document.querySelector('input[value="ichimoku"]').checked;

	let extra = 0;

	// ボリンジャーバンド用に余分なデータ期間を確保
	if (isBbandsChecked) {
		const periods = stockConfig.getSMAPeriods(); // 例：[5, 25, 75]
		extra = periods[1]; // 中央の期間を使用 → 25
	}

	// 一目均衡表用に最大必要期間を確保（先行スパンB + 基準線）
	if (isIchimokuChecked) {
		const ichi = stockConfig.getIchimokuPeriods(); // {tenkan, kijun, senkouB}
		extra = Math.max(extra, ichi.senkouB + ichi.kijun); // 通常 52 + 26 = 78
	}

	const data = await fetchStockData(extra); // データ取得（余分な日数を含める）

	// x軸用のラベル（日付）
	if (data.length == stockConfig.outputsize) {
			labels = data.map(d => d.datetime);
		}
		else {
			//データの数が200より少ない場合はappleからlableをとる
			const beforeSymbol = stockConfig.symbol;
			stockConfig.symbol = "AAPL";
			const appleData = await fetchStockData();
			labels = appleData.map(d => d.datetime);

			stockConfig.symbol = beforeSymbol;
		}
		
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

	if (isSmaChecked || isBbandsChecked) {
		let SMAResults = await fetchSMAData(extra, data.length);

		SMAResults = SMAResults.map(sma => ({
			...sma,
			values: sma.values.map(d => {
				const smaValue = parseFloat(d.indicators?.sma ?? 0);
				return {
					...d,
					indicators: {
						...d.indicators,
						sma: smaValue === 0 ? null : smaValue
					}
				};
			})
		}));

		SMADatasets = SMAResults.map(sma => ({
			type: "line",
			label: `SMA (${sma.timeperiod})`,
			data: sma.values
				.map(d => ({ x: d.datetime, y: parseFloat(d.indicators.sma) })),
			borderColor: chartStyleConfig.getSMAColor(sma.timeperiod),
			borderWidth: 2,
			pointRadius: 0,
			fill: false,
			order: 10
		}));

		if (isBbandsChecked) {
			const midSMA = SMAResults[1]; // 中央線（例：25期間）
			const smaArray = midSMA.values.map(d => ({
				x: d.datetime,
				y: parseFloat(d.indicators.sma)
			}));

			const closePrices = candleData.map(d => d.c); // 終値配列

			// ボリンジャーバンドの上下線を計算
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
					borderDash: [5, 5]
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
					borderDash: [5, 5]
				}
			];

			// 出力サイズに合わせて切り捨て
			bbandsDatasets = bbandsDatasets.map(ds => ({
				...ds,
				data: ds.data.slice(-stockConfig.outputsize)
			}));

			// SMAが未チェックなら中央線は表示しない
			if (!isSmaChecked) {
				SMADatasets = [];
			}
		}
	}

	// 一目均衡表がチェックされている場合の処理
	if (isIchimokuChecked) {
		const ichimokuPeriods = stockConfig.getIchimokuPeriods();
		const ichimoku = calculateIchimoku(candleData, {
			tenkanPeriod: ichimokuPeriods.tenkan,
			kijunPeriod: ichimokuPeriods.kijun,
			senkouBPeriod: ichimokuPeriods.senkouB,
			chikouOffsetPeriod: ichimokuPeriods.span
		});

		// 一目均衡表のラインを設定
		ichimokuDatasets = [
			{
				type: "line",
				label: "転換線",
				data: ichimoku.tenkan,
				borderColor: "orange",
				borderWidth: 1,
				pointRadius: 0,
				order: 5
			},
			{
				type: "line",
				label: "基準線",
				data: ichimoku.kijun,
				borderColor: "blue",
				borderWidth: 1,
				pointRadius: 0,
				order: 5
			},
			{
				type: "line",
				label: "先行線1",
				data: ichimoku.senkouA,
				borderColor: "green",
				borderWidth: 1,
				pointRadius: 0,
				fill: {
					target: "+1",
					above: "rgba(144,238,144,0.4)",
					below: "rgba(255,182,193,0.4)"
				},
				order: 4
			},
			{
				type: "line",
				label: "先行線2",
				data: ichimoku.senkouB,
				borderColor: "red",
				borderWidth: 1,
				pointRadius: 0,
				order: 4
			},
			{
				type: "line",
				label: "遅行線",
				data: ichimoku.chikou,
				borderColor: "purple",
				borderWidth: 1,
				pointRadius: 0,
				order: 3
			}
		];


		// 一目均衡表は未来の分も含めて表示する必要があるため、span分を含めてスライス
		ichimokuDatasets = ichimokuDatasets.map(ds => {
			return {
				...ds,
				data: ds.data.slice(-stockConfig.outputsize - ichimokuPeriods.span)
			};
		});
	}

	// 再描画のため、既存のチャートがあれば破棄
	if (candleChart) {
		candleChart.destroy();
	}
	if (volumeChart) {
		volumeChart.destroy();
	}

	// 計算用に多く取得したデータを出力サイズ分に切り詰め
	if (isBbandsChecked || isIchimokuChecked) {
		SMADatasets = SMADatasets.map(ds => ({
			...ds,
			data: ds.data.slice(-stockConfig.outputsize)
		}));

		candleData = candleData.slice(-stockConfig.outputsize);
		volumeData = volumeData.slice(-stockConfig.outputsize);
		labels = labels.slice(-stockConfig.outputsize);


		// 一目均衡表がオンなら未来の日付をlabelsに追加（先行スパン描画用）
		const lastDate = labels[labels.length - 1];
		if (isIchimokuChecked) {
			for (let i = 1; i < 25; i++) {
				const futureDate = addDays(lastDate, i); // 実際の日付を使用
				labels.push(futureDate);
			}
		}
	}
	
	//データ量がlabelsと相違する場合nullで補足
	if (labels.length != candleData.length) {
		candleData = padCandleDataToLabels(labels, candleData);
		volumeData = padVolumeDataToLabels(labels, volumeData);

		SMADatasets.forEach(ds => ds.data = padDataToLabels(labels, ds.data));
		ichimokuDatasets.forEach(ds => ds.data = padDataToLabels(labels, ds.data));
		bbandsDatasets.forEach(ds => ds.data = padDataToLabels(labels, ds.data));
	}

	// チャートを生成・描画
	candleChart = createCandleChart(labels, candleData, volumeData, SMADatasets, bbandsDatasets, ichimokuDatasets);
	volumeChart = createVolumeChart(labels, volumeData);
	setTimeout(() => {
		enableTrendlineDrawing(candleChart);
	}, 100);
}

// ローソク足チャートの作成関数

const createCandleChart = (labels, data, volumeData, SMADatasets, bbandsDatasets, ichimokuDatasets) => {
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
			...bbandsDatasets,
			...ichimokuDatasets
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
							tooltipEl = document.getElementById('custom-tooltip');
							if (!tooltipEl) {
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
						}
						// コンテンツ描画
						const tooltipItems = tooltip.dataPoints;
						const title = tooltip.title?.[0] ?? '';
						
						//titleを追加
						let html = `<div style="margin-bottom: 6px; font-weight: bold;">${title}</div>`;

						tooltipItems.forEach((ctx) => {
							const item = ctx.raw;
							const dataset = ctx.dataset;
							const color = dataset.borderColor || '#fff';


							//ロウソク足データがある場合はlabelsに表示
							if (item && item.o != null && item.h != null && item.l != null && item.c != null) {
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
							} else if (item && item.y !== undefined) {　//ロウソク足以外のladels設定
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


function addDays(dateStr, days) {
	const date = new Date(dateStr);
	date.setDate(date.getDate() + days);
	return date.toISOString().slice(0, 10);
}

const padDataToLabels = (labels, rawData) =>
	labels.map(x => {
		const found = rawData.find(d => d.x === x);
		return { x, y: found ? found.y : null };
	});

const padCandleDataToLabels = (labels, rawData) =>
	labels.map(x => {
		const found = rawData.find(d => d.x === x);
		return found
			? { x, o: found.o, h: found.h, l: found.l, c: found.c }
			: { x, o: null, h: null, l: null, c: null };
	});

const padVolumeDataToLabels = (labels, rawData) =>
	labels.map(x => {
		const found = rawData.find(d => d.x === x);
		return { x, y: found?.y ?? null };
	});

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