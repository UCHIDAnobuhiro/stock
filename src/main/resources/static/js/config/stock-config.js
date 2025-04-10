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
}

export default new StockConfig();
