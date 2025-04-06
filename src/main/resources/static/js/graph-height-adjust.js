//このjsはグラフのdivの高さを具体的な数値に設定する
//（chart.jsの容器は動的な高さ（h-full,h-1/2とか）を設定できないため、数値をいじる必要がある）
const adjustChartHeights = () => {
	//初期設定
    const containerHeight = document.getElementById('chartContainer').clientHeight;
	//7:3　余裕を持つために 0.69:0.29にする　修正するときも0.01を
    const candleChartHeight = containerHeight * 0.69;
    const volumeChartHeight = containerHeight * 0.29;
	console.log(containerHeight);
	//グラフの高さを設定
    document.getElementById('candlestick-chart-container').style.height = `${candleChartHeight}px`;
    document.getElementById('volume-chart-container').style.height = `${volumeChartHeight}px`;
};

// ロード時の初期化設定
window.addEventListener('load', adjustChartHeights);

//サイズを更新中なの変数とタイマー
let resizeTimeout = null;
let isResizing = false;

const handleResize = () => {
    // resizeする時はまず幅を0に変更
    if (!isResizing) {
        document.getElementById('candlestick-chart-container').style.height = '0px';
        document.getElementById('volume-chart-container').style.height = '0px';
        isResizing = true;
    }

    // requestAnimationFrameでまだ幅を変更中なのかをチェック
	if (resizeTimeout) {
        cancelAnimationFrame(resizeTimeout);
    }

	//調整完了なら
    resizeTimeout = requestAnimationFrame(() => {
        isResizing = false; 
        adjustChartHeights(); // 幅更新する
    });
};

// 画面がresizeするときのメソッド呼び出し
window.addEventListener('resize', handleResize);
