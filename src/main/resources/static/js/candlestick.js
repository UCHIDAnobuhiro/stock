const rawData =
	[
		{
			"datetime": "2025-03-26",
			"open": 223.50999,
			"high": 225.020004,
			"low": 220.47,
			"close": 221.53,
			"volume": 34466100
		},
		{
			"datetime": "2025-03-25",
			"open": 220.77,
			"high": 224.10001,
			"low": 220.080002,
			"close": 223.75,
			"volume": 34493600
		},
		{
			"datetime": "2025-03-24",
			"open": 221,
			"high": 221.48,
			"low": 218.58,
			"close": 220.73,
			"volume": 44299500
		},
		{
			"datetime": "2025-03-21",
			"open": 211.56,
			"high": 218.84,
			"low": 211.28,
			"close": 218.27,
			"volume": 94127800
		},
		{
			"datetime": "2025-03-20",
			"open": 213.99001,
			"high": 217.49001,
			"low": 212.22,
			"close": 214.10001,
			"volume": 48862900
		},
		{
			"datetime": "2025-03-19",
			"open": 214.22,
			"high": 218.75999,
			"low": 213.75,
			"close": 215.24001,
			"volume": 54385400
		},
		{
			"datetime": "2025-03-18",
			"open": 214.16,
			"high": 215.14999,
			"low": 211.49001,
			"close": 212.69,
			"volume": 42432400
		},
		{
			"datetime": "2025-03-17",
			"open": 213.31,
			"high": 215.22,
			"low": 209.97,
			"close": 214,
			"volume": 48073400
		},
		{
			"datetime": "2025-03-14",
			"open": 211.25,
			"high": 213.95,
			"low": 209.58,
			"close": 213.49001,
			"volume": 60107600
		},
	];

// 古い順にソート
rawData.sort((a, b) => new Date(a.datetime) - new Date(b.datetime));

const labels = rawData.map(d => d.datetime);

const candleData = rawData.map(d => ({
	x: d.datetime,
	o: d.open,
	h: d.high,
	l: d.low,
	c: d.close
}));

const volumeData = rawData.map(d => ({
	x: d.datetime,
	y: d.volume
}));

const ctx = document.getElementById("candlestick-chart").getContext("2d");

const chart = new Chart(ctx, {
	type: "candlestick",
	data: {
		labels: labels, // カテゴリ軸で使う
		datasets: [
			{
				label: "価格",
				data: candleData,
				borderColor: { up: "#26a69a", down: "#ef5350" },
				backgroundColor: { up: "#26a69a", down: "#ef5350" },
			}
		]
	},
	options: {
		responsive: true,
		scales: {
			x: {
				type: "category",
				stacked: false,
				labels: labels,
				title: { display: true, text: "日付" },
				ticks: {
					maxRotation: 0,
					autoSkip: true
				}
			},
			y: {
				position: "left",
				title: { display: true, text: "価格" },
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
			}
		}
	}
});	

// 出来高チャート
    new Chart(document.getElementById("volume-chart").getContext("2d"), {
      type: "bar",
      data: {
        labels: labels,
        datasets: [{
          label: "出来高",
          data: volumeData,
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
            title: { display: true, text: "出来高" },
            ticks: {
              callback: v => `${v / 1_000_000}M`
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