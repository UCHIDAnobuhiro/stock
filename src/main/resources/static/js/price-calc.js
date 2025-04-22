document.addEventListener("DOMContentLoaded", () => {
    const quantityInput = document.querySelector('input[name="quantity"]');
    const unitPriceInput = document.querySelector('input[name="unitPrice"]');
    const exchangeRateSpan = document.getElementById('exchangeRate');
    const priceDisplay = document.getElementById('price');
    const moneySelectors = document.querySelectorAll('input[name="moneySelector"]');
    const priceTypeRadios = document.querySelectorAll('input[name="priceType"]');

    const setUnitPriceRange = () => {
        const basePrice = parseFloat(unitPriceInput.getAttribute('value')) || 0;
        const min = (basePrice * 0.9).toFixed(2);
        const max = (basePrice * 1.1).toFixed(2);
        unitPriceInput.min = min;
        unitPriceInput.max = max;
    };

    const calculatePrice = () => {
        const quantity = parseFloat(quantityInput.value) || 0;
        const unitPrice = parseFloat(unitPriceInput.value) || 0;
        const exchangeRate = parseFloat(exchangeRateSpan.dataset.rate) || 1;
        const selectedCurrency = document.querySelector('input[name="moneySelector"]:checked')?.value;
        const selectedPriceType = document.querySelector('input[name="priceType"]:checked')?.value;

        if (selectedPriceType === "LIMIT") {
            if (selectedCurrency === "JPY") {
                const jpy = quantity * unitPrice * exchangeRate;
                priceDisplay.textContent = `${jpy.toLocaleString(undefined, { maximumFractionDigits: 2 })}円`;
            } else {
                const usd = quantity * unitPrice;
                priceDisplay.textContent = `${usd.toFixed(2)}ドル`;
            }
        } else {
            priceDisplay.textContent = "成行注文のため金額は変動します";
        }
    };

    quantityInput.addEventListener('input', calculatePrice);
    unitPriceInput.addEventListener('input', calculatePrice);
    moneySelectors.forEach(radio => radio.addEventListener('change', calculatePrice));
    priceTypeRadios.forEach(radio => radio.addEventListener('change', calculatePrice));

    setUnitPriceRange();  // 设置限制
    calculatePrice();     // 初始计算
});
