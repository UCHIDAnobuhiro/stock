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
			}
		});
	});
});