import { setSymbol, renderCharts } from './candlestick.js';
//お気に入りボタンを押下時　TRはinnerHTMLで一部更新され、DOMがリセットされeventlistenerがなくなる
//そのために　TRの親TBにlistenerを追加する
document.addEventListener('DOMContentLoaded', () => {
	const stocksTB = document.querySelector("#stocksTB");
	if (stocksTB) {
	    stocksTB.addEventListener('click', onRowClick);
	}
});

const onRowClick = (event) => {
	//clickable-rowの押下時だけ動く
	const row = event.target.closest('.clickable-row');

	// チェックボックスは無視
	if (!row || event.target.closest('input')) return;
	const ticker = row.dataset.ticker;
	const brand = row.dataset.brand;
	console.log('クリックされた銘柄:', ticker, brand);
	setSymbol(ticker);
	renderCharts();

	// 銘柄情報を非同期で取得して挿入
	fetch(`/stock/table?symbol=${encodeURIComponent(ticker)}`)
		.then(res => res.text())
		.then(html => {
			console.log('取得したHTML:', html); // ← ここで確認！
			document.getElementById('todayInformation').innerHTML = html;
			document.getElementById('tickerNameAndCode').innerText = `${brand} (${ticker})`;
		})
		.catch(err =>
			console.error('銘柄情報取得失敗:', err));
};
