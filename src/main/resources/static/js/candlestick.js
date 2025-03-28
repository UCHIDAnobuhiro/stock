const symbol = 'AAPL';
const interval = '1day';

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

	createCandleChart(labels, candleData);
	createVolumeChart(labels, volumeData);
}

const createCandleChart = (labels, data) => {
	new Chart(document.getElementById("candlestick-chart").getContext("2d"), {
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
					stacked: false,
					labels: labels,
					ticks: {
						maxRotation: 0,
						autoSkip: true
					}
				},
				y: {
					position: "right",
					ticks: {
					  callback: (value) => value.toFixed(2)
					}
				}
			},
			plugins: {
				tooltip: {
					callbacks: {
						title: (context) => context[0].label,
						label: (context) => {
							const item = context.raw;
							return [
								`始値: ${item.o}`,
								`高値: ${item.h}`,
								`安値: ${item.l}`,
								`終値: ${item.c}`,
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
	new Chart(document.getElementById("volume-chart").getContext("2d"), {
		type: "bar",
		data: {
			labels,
			datasets: [{
				label: "出来高",
				data,
				backgroundColor: "rgba(100, 149, 237, 0.4)",
				borderColor: "rgba(100, 149, 237, 1)",
				barThickness: 8
			}]
		},
		options: {
			responsive: true,
			scales: {
				x: {
					type: "category",
					labels: labels,
					ticks: { maxRotation: 0, autoSkip: true },
					grid: { display: false }
				},
				y: {
					position: "right",
					ticks: {
						callback: v => `${v / 1_000}K`
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

// DOM読み込み後にチャートを描画
document.addEventListener("DOMContentLoaded", renderCharts);

