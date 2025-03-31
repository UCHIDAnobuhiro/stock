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

	createCombinedChart(labels, candleData, volumeData);
}

const createCombinedChart = (labels, candleData, volumeData) => {
	new Chart(document.getElementById("combined-chart").getContext("2d"), {
		type: 'candlestick',
		data: {
			labels,
			datasets: [
				{
					type: 'candlestick',
					label: '価格',
					data: candleData,
					yAxisID: 'y1',
					borderColor: { up: '#26a69a', down: '#ef5350' },
					backgroundColor: { up: '#26a69a', down: '#ef5350' }
				},
				{
					type: 'bar',
					label: '出来高',
					data: volumeData,
					yAxisID: 'y2',
					backgroundColor: 'rgba(100, 149, 237, 0.4)',
					borderColor: 'rgba(100, 149, 237, 1)',
					barThickness: 4
				}
			]
		},
		options: {
			responsive: true,
			scales: {
				x: {
					type: 'category',
					ticks: { maxRotation: 0, autoSkip: true }
				},
				y1: {
					position: 'right',
					type: 'linear',
					weight: 3,
					ticks: {
						callback: value => value.toFixed(2)
					},
					title: { display: false }
				},
				y2: {
					position: 'left',
					weight: 1,
					grid: { drawOnChartArea: false },
					ticks: {
						callback: value => `${value / 1_000}K`
					},
					title: { display: false }
				}
			},
			plugins: {
				tooltip: {
					mode: 'index',
					intersect: false,
					callbacks: {
						label: ctx => {
							if (ctx.dataset.type === 'candlestick') {
								const item = ctx.raw;
								return [
									`始値: ${item.o}`,
									`高値: ${item.h}`,
									`安値: ${item.l}`,
									`終値: ${item.c}`
								];
							} else {
								return `出来高: ${ctx.raw.y.toLocaleString()}`;
							}
						}
					}
				},
				legend: { display: false }
			}
		}
	});
};

// DOM読み込み後にチャートを描画
document.addEventListener("DOMContentLoaded", renderCharts);

