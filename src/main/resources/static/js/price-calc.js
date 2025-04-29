document.addEventListener("DOMContentLoaded", () => {
	// --- 要素取得 ---
	const quantityInput = document.querySelector('input[name="quantity"]');
	const unitPriceInput = document.querySelector('input[name="unitPrice"]');
	const exchangeRateSpan = document.getElementById('exchangeRate');
	const priceDisplay = document.getElementById('totalPriceDisplay');
	const moneySelectors = document.querySelectorAll('input[name="settlementCurrency"]');
	const priceTypeRadios = document.querySelectorAll('input[name="type"]');
	const stockCloseSpan = document.getElementById('stockClose');
	const stockClose = parseFloat(stockCloseSpan.dataset.close); // 現在の株価
	const hiddenExchangeRateInput = document.getElementById('hiddenExchangeRate');
	const side = document.querySelector('input[name="side"]')?.value; // "buy" または "sell"
	const toggleBtn = document.getElementById('toggleDetails');
	const details = document.getElementById('calculationDetails');

	// --- 指値・成行による単価入力可否切替 ---
	const toggleUnitPriceState = () => {
		const selectedPriceType = document.querySelector('input[name="type"]:checked')?.value;
		if (selectedPriceType === "MARKET") {
			unitPriceInput.readOnly = true;
			unitPriceInput.classList.add("bg-gray-300", "cursor-not-allowed"); // 成行なら入力不可
		} else {
			unitPriceInput.readOnly = false;
			unitPriceInput.classList.remove("bg-gray-300", "cursor-not-allowed"); // 指値なら入力可能
		}
	};

	// --- 金額計算・詳細式の更新 ---
	const calculatePrice = () => {
		const quantity = parseFloat(quantityInput.value) || 0;
		const unitPrice = parseFloat(unitPriceInput.value) || 0;
		const exchangeRate = parseFloat(exchangeRateSpan.dataset.rate) || 1;
		const selectedCurrency = document.querySelector('input[name="settlementCurrency"]:checked')?.value;
		const selectedPriceType = document.querySelector('input[name="type"]:checked')?.value;
		let total = 0;
		let detailText = "";
		let formulaText = "";

		if (selectedPriceType === "LIMIT") {
			// --- 指値注文の場合 ---
			if (selectedCurrency === "JPY") {
				total = quantity * unitPrice * exchangeRate;
				priceDisplay.textContent = `${Math.round(total).toLocaleString()}円`;
				detailText = `${quantity} 株 × ${unitPrice.toFixed(2)} ドル × ${exchangeRate.toFixed(2)} 円<br>= 約 ${Math.round(total).toLocaleString()} 円`;
				formulaText = `<br><small class="text-gray-500">株数 × 価格 × 参考為替レート</small>`;
			} else {
				total = quantity * unitPrice;
				priceDisplay.textContent = `${total.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}ドル`;
				detailText = `${quantity} 株 × ${unitPrice.toFixed(2)} ドル<br>= ${total.toFixed(2)} ドル`;
				formulaText = `<br><small class="text-gray-500">株数 × 価格</small>`;
			}
		} else if (selectedPriceType === "MARKET") {
			// --- 成行注文の場合 ---
			const multiplier = side === "buy" ? 1.1 : 0.9; // 買いなら1.1倍、売りなら0.9倍

			if (selectedCurrency === "JPY") {
				total = quantity * stockClose * multiplier * exchangeRate;
				priceDisplay.textContent = `${Math.round(total).toLocaleString()}円`;
				detailText = `${quantity} 株 × (${stockClose.toFixed(2)} ドル × ${multiplier}) × ${exchangeRate.toFixed(2)} 円<br>= 約 ${Math.round(total).toLocaleString()} 円`;
				formulaText = `<br><small class="text-gray-500">株数 × (現在値 × 成行倍率) × 参考為替レート</small>`;
			} else {
				total = quantity * stockClose * multiplier;
				priceDisplay.textContent = `${total.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}ドル`;
				detailText = `${quantity} 株 × (${stockClose.toFixed(2)} ドル × ${multiplier})<br>= ${total.toFixed(2)} ドル`;
				formulaText = `<br><small class="text-gray-500">株数 × (現在値 × 成行倍率)</small>`;
			}
		}

		// --- 無効な入力時 ---
		if (quantity <= 0 || unitPrice <= 0) {
			priceDisplay.textContent = "0";
			detailText = "数量または単価が未入力です";
			formulaText = "";
		}

		// 隠し為替レートinputにセット
		hiddenExchangeRateInput.value = exchangeRate;

		// 詳細式を更新
		details.innerHTML = detailText + formulaText;
	};

	// --- イベントリスナー登録 ---
	quantityInput.addEventListener('input', calculatePrice);
	unitPriceInput.addEventListener('input', calculatePrice);
	moneySelectors.forEach(radio => radio.addEventListener('change', calculatePrice));
	priceTypeRadios.forEach(radio => {
		radio.addEventListener('change', () => {
			toggleUnitPriceState();
			calculatePrice();
		});
	});

	// --- ▼ボタンによる詳細開閉 ---
	toggleBtn.addEventListener('click', () => {
		details.classList.toggle('hidden');
		toggleBtn.textContent = details.classList.contains('hidden') ? '▼' : '▲';
	});

	// --- 初期状態設定 ---
	toggleUnitPriceState();
	calculatePrice();
});
