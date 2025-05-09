// trade-search.js

const searchTrades = () => {
	const date = document.getElementById('dateFilter').value;
	const ticker = document.getElementById('tickerFilter').value;

	const url = `/trade-log/search?date=${date}&ticker=${encodeURIComponent(ticker)}`;
	fetch(url)
		.then(res => res.text())
		.then(html => {
			document.getElementById('trade-log-table').innerHTML = html;
		})
		.catch(err => {
			console.error('検索失敗:', err);
		});
};

// ページロード時にイベントリスナーを登録
document.addEventListener('DOMContentLoaded', () => {
	const searchButton = document.getElementById('tradeSearchBtn');
	if (searchButton) {
		searchButton.addEventListener('click', searchTrades);
	}
});
