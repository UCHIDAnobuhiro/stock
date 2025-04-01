import { setSymbol, renderCharts } from './candlestick.js';

document.addEventListener('DOMContentLoaded', () => {

	document.querySelectorAll('.clickable-row').forEach(row => {
		row.addEventListener('click', (event) => {
			// チェックボックスは無視
			if (!event.target.closest('input')) {
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
					.catch(err => {
						console.error('銘柄情報取得失敗:', err);
					});
			}
		});
	});
});