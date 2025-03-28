let symbol = 'AAPL';
let interval = '1day';

let candleChart = null;
let volumeChart = null;

const fetchStockData = async () => {
	const url = `http://localhost:8080/api/stocks/time-series/values?symbol=${symbol}&interval=${interval}`
	const res = await fetch(url);
	const json = await res.json();

	if (json.status === "error") {
		console.error("API error:", json.message);
		return;
	}
	const rawData = json.reverse();
	return rawData;
}

const renderCharts = async () => {
	const data = await fetchStockData();

	const labels = data.map(d => d.datetime);

	const candleData = data.map(d => ({
		x: d.datetime,
		o: d.open,
		h: d.high,
		l: d.low,
		c: d.close
	}));

	const volumeData = data.map(d => ({
		x: d.datetime,
		y: d.volume
	}));

	// 再生成
	if (candleChart) {
		candleChart.destroy();
	}
	if (volumeChart) {
		volumeChart.destroy();
	}

	candleChart = createCandleChart(labels, candleData, volumeData);
	volumeChart = createVolumeChart(labels, volumeData);
}

const createCandleChart = (labels, data, volumeData) => {
	return new Chart(document.getElementById("candlestick-chart").getContext("2d"), {
		type: "candlestick",
		data: {
			labels,
			datasets: [{
				label: "価格",
				data,
				borderColor: { up: "#26a69a", down: "#ef5350" },
				backgroundColor: { up: "#26a69a", down: "#ef5350" }
			}]
		},
		options: {
			responsive: true,
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
						callback: (value) => value.toFixed(2)
					},
					afterFit: scale => {
						scale.width = 70;
					}
				}
			},
			plugins: {
				tooltip: {
					callbacks: {
						title: (context) => context[0].label,
						label: (context) => {
							const item = context.raw;
							const matchedVolume = volumeData.find(v => v.x === item.x);
							const volume = matchedVolume ? matchedVolume.y.toLocaleString() : "N/A";
							return [
								`始値: ${item.o}`,
								`高値: ${item.h}`,
								`安値: ${item.l}`,
								`終値: ${item.c}`,
								`出来高: ${volume}`
							];
						}
					}
				},
				legend: { display: false }
			}
		}
	});
}

const createVolumeChart = (labels, data) => {
	return new Chart(document.getElementById("volume-chart").getContext("2d"), {
		type: "bar",
		data: {
			labels,
			datasets: [{
				label: "出来高",
				data,
				backgroundColor: "rgba(100, 149, 237, 0.4)",
				borderColor: "rgba(100, 149, 237, 1)",
				barThickness: 5
			}]
		},
		options: {
			responsive: true,
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
						display: false
					}
				},
				y: {
					position: "right",
					ticks: {
						callback: v => v === 0 ? "0" : `${v / 1_000}K`
					},
					afterFit: scale => {
						scale.width = 70;
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

// セレクタ変更イベントで interval 更新＋再描画
document.getElementById("candleSelector").addEventListener("change", (e) => {
	interval = e.target.value;
	renderCharts();
});

// DOM読み込み後にチャートを描画
document.addEventListener("DOMContentLoaded", renderCharts);

