class StockConfig {
  #symbol = 'AAPL'; // ＃symbolはsymbolをprivateに設定
  #interval = '1day';
  #outputsize = 100;

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

}

export default new StockConfig();
