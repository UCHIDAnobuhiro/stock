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
