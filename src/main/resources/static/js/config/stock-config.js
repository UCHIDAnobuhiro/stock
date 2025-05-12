class StockConfig {
	#symbol = 'AAPL'; // ＃symbolはsymbolをprivateに設定
	#interval = '1day';
	#outputsize = 200;
	//移動平均線の設定、日足の場合は5,25,75の三本を表示する
	#smaPeriodMap = {
		"1day": [5, 25, 75],
		"1week": [13, 26, 52],
		"1month": [9, 24, 60]
	};
	#ichimokuPeriodMap = {
		//転換線期間、kijun+1=基準線期間、先行線、span+1=スパン期間
		"1day": { tenkan: 9, kijun: 25, senkouB: 52, span: 25 },
		"1week": { tenkan: 13, kijun: 25, senkouB: 52, span: 25 },
		"1month": { tenkan: 9, kijun: 23, senkouB: 60, span: 25 }
	};

	get symbol() { return this.#symbol; }
	set symbol(v) { this.#symbol = v; }
	get interval() { return this.#interval; }
	set interval(v) { this.#interval = v; }
	get outputsize() { return this.#outputsize; }
	set outputsize(v) { this.#outputsize = v; }

	getSMAPeriods() {
		return this.#smaPeriodMap[this.#interval] || [];
	}

	//set SMAPeriods()は一つのパラメータまでなので、メソッドのほうがわかりやすい。
	setSMAPeriods(interval, periods) {
		this.#smaPeriodMap[interval] = periods;
	}

	getIchimokuPeriods() {
		return this.#ichimokuPeriodMap[this.#interval] || { tenkan: 9, kijun: 25, senkouB: 52, span: 26 };
	}
	setIchimokuPeriods(interval, config) {
		this.#ichimokuPeriodMap[interval] = config;
	}
	
	/**
	* ページ内の DOM から ticker を取得し symbol を初期化する
	*/
	initFromDOM() {
		const el = document.getElementById("tickerNameAndCode");
		if (el) {
			const text = el.innerText;
			const match = text.match(/\(([^)]+)\)/); // 括弧内の ticker を抽出
			if (match && match[1]) {
				this.#symbol = match[1];
				console.log("StockConfig: symbol 初期化成功:", this.#symbol);
			} else {
				console.warn("StockConfig: ticker コードが見つかりません");
			}
		} else {
			console.warn("StockConfig: #tickerNameAndCode 要素が存在しません");
		}
	}
}

export default new StockConfig();
