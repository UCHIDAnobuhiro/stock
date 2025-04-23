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

	const toggleUnitPriceState = () => {
		const selectedPriceType = document.querySelector('input[name="type"]:checked')?.value;

		if (selectedPriceType === "MARKET") {
			unitPriceInput.readOnly = true;
			unitPriceInput.classList.add("bg-gray-300","cursor-not-allowed");
		} else {
			unitPriceInput.readOnly = false;
			unitPriceInput.classList.remove("bg-gray-300","cursor-not-allowed");
		}
	};

	const calculatePrice = () => {
		const quantity = parseFloat(quantityInput.value) || 0;
		const unitPrice = parseFloat(unitPriceInput.value) || 0;
		const exchangeRate = parseFloat(exchangeRateSpan.dataset.rate) || 1;
		const selectedCurrency = document.querySelector('input[name="settlementCurrency"]:checked')?.value;
		const selectedPriceType = document.querySelector('input[name="type"]:checked')?.value;

		let total = 0;
		
		if (selectedPriceType === "LIMIT") {
			if (selectedCurrency === "JPY") {
				total = quantity * unitPrice * exchangeRate;
				priceDisplay.textContent = `${Math.round(total).toLocaleString()}円`;
			} else {
				total = quantity * unitPrice;
				priceDisplay.textContent = `${total.toFixed(2)}ドル`;
			}
		} else if (selectedPriceType === "MARKET") {
			const marketUnitPrice = stockClose * (side === "buy" ? 1.1 : 0.9);

			if (selectedCurrency === "JPY") {
				total = quantity * marketUnitPrice * exchangeRate;
				priceDisplay.textContent = `${Math.round(total).toLocaleString()}円`;
			} else {
				total = quantity * marketUnitPrice;
				priceDisplay.textContent = `${total.toFixed(2)}ドル`;
			}
		}
		
		if(quantity<=0||unitPrice<=0){
			priceDisplay.textContent="0";
		}

		hiddenExchangeRateInput.value = exchangeRate;
	};

	quantityInput.addEventListener('input', calculatePrice);
	unitPriceInput.addEventListener('input', calculatePrice);
	moneySelectors.forEach(radio => radio.addEventListener('change', calculatePrice));
	priceTypeRadios.forEach(radio => {
		radio.addEventListener('change', () => {
			toggleUnitPriceState();
			calculatePrice();
		});
	});

	toggleUnitPriceState();
	calculatePrice();
});
