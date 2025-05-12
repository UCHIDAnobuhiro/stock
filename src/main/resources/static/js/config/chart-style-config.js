class ChartStyleConfig {
	#smaColorMap = {
		5: "#42a5f5",
		25: "#66bb6a",
		75: "#ffa726",
		13: "#ab47bc",
		26: "#26a69a",
		52: "#ef5350",
		9: "#ec407a",
		24: "#26c6da",
		60: "#8d6e63"
	};
	#trendLineBorderColor = "rgba(0, 0, 0, 1)";
	#trendLineBorderWidth = 2;
	#showAmount =100;
	#ticksSkipPadding =5;

	get trendLineBorderColor() { return this.#trendLineBorderColor; }
	set trendLineBorderColor(v) { this.#trendLineBorderColor = v; }
	get trendLineBorderWidth() { return this.#trendLineBorderWidth; }
	set trendLineBorderWidth(v) { this.#trendLineBorderWidth = v; }
	get showAmount() { return this.#showAmount; }
	set showAmount(v) { this.#showAmount = v; }
	get ticksSkipPadding() { return this.#ticksSkipPadding; }
	set ticksSkipPadding(v) { this.#ticksSkipPadding = v; }


	getSMAColor(period) {
		return this.#smaColorMap[period] || "#999"; // fallback color
	}

	setSMAColor(period, color) {
		this.#smaColorMap[period] = color;
	}
}

export default new ChartStyleConfig();
