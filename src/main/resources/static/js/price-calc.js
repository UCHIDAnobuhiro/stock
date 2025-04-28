document.addEventListener("DOMContentLoaded", () => {
	const quantityInput = document.querySelector('input[name="quantity"]');
	const unitPriceInput = document.querySelector('input[name="unitPrice"]');
	const exchangeRateSpan = document.getElementById('exchangeRate');
	const priceDisplay = document.getElementById('totalPriceDisplay');
	const moneySelectors = document.querySelectorAll('input[name="settlementCurrency"]');
	const priceTypeRadios = document.querySelectorAll('input[name="type"]');
	const stockCloseSpan = document.getElementById('stockClose');
	const stockClose = parseFloat(stockCloseSpan.dataset.close);
	const hiddenExchangeRateInput = document.getElementById('hiddenExchangeRate');
	const side = document.querySelector('input[name="side"]')?.value;
	const toggleBtn = document.getElementById('toggleDetails');
	const details = document.getElementById('calculationDetails');

	// 指値/成行による単価input可否切替
	const toggleUnitPriceState = () => {
		const selectedPriceType = document.querySelector('input[name="type"]:checked')?.value;
		if (selectedPriceType === "MARKET") {
			unitPriceInput.readOnly = true;
			unitPriceInput.classList.add("bg-gray-300", "cursor-not-allowed");
		} else {
			unitPriceInput.readOnly = false;
			unitPriceInput.classList.remove("bg-gray-300", "cursor-not-allowed");
		}
	};

	// 価格、詳細式の更新
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
			// 指値注文
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
			// 成行注文，直接在公式中带倍率
			const multiplier = side === "buy" ? 1.1 : 0.9;

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

		// 输入不合法时
		if (quantity <= 0 || unitPrice <= 0) {
			priceDisplay.textContent = "0";
			detailText = "数量または単価が未入力です";
			formulaText = "";
		}

		hiddenExchangeRateInput.value = exchangeRate;

		// 更新公式区块
		details.innerHTML = detailText + formulaText;
	};

	// イベント設定
	quantityInput.addEventListener('input', calculatePrice);
	unitPriceInput.addEventListener('input', calculatePrice);
	moneySelectors.forEach(radio => radio.addEventListener('change', calculatePrice));
	priceTypeRadios.forEach(radio => {
		radio.addEventListener('change', () => {
			toggleUnitPriceState();
			calculatePrice();
		});
	});

	// ▼按钮展开/收起公式
	toggleBtn.addEventListener('click', () => {
		details.classList.toggle('hidden');
		toggleBtn.textContent = details.classList.contains('hidden') ? '▼' : '▲';
	});

	// 初始化
	toggleUnitPriceState();
	calculatePrice();
});
