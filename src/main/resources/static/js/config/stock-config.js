class StockConfig {
  #symbol = 'AAPL'; // ＃symbolはsymbolをprivateに設定
  #interval = '1day';
  #outputsize = 100;
  #apiBaseUrl = '/api/stocks/time-series/values';

  get symbol() {
    return this.#symbol;
  }

  set symbol(value) {
    this.#symbol = value;
  }

  get interval() {
    return this.#interval;
  }

  set interval(value) {
    this.#interval = value;
  }

  get outputsize() {
    return this.#outputsize;
  }

  set outputsize(value) {
    this.#outputsize = value;
  }

  get apiBaseUrl() {
    return this.#apiBaseUrl;
  }
}

export default new StockConfig();
