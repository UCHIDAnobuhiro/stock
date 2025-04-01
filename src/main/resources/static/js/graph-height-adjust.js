//このjsはグラフのdivの高さを具体的な数値に設定する（chart.jsを動的高さを調整するためです）
const adjustChartHeights = () => {
	// 親divの高さを取得
	const containerHeight = document.getElementById('chartContainer').clientHeight;

	// 7:3にする、余裕を持つために6.9:2.9
	const candleChartHeight = containerHeight * 0.69;
	const volumeChartHeight = containerHeight * 0.29;

	// 各グラフの高さを調整
	document.getElementById('candlestick-chart-container').style.height = `${candleChartHeight}px`;
	document.getElementById('volume-chart-container').style.height = `${volumeChartHeight}px`;
}

// 画面がロードするときに高さを調整
window.addEventListener('load', adjustChartHeights);
