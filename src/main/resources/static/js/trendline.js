import chartStyleConfig from './config/chart-style-config.js'; // グラフのスタイル設定ファイルをインポート

export const trendlineAnnotations = {}; // すべてのトレンドラインを保存するオブジェクト

let start = null; // 描画の開始点
let lineId = 0; // ラインIDカウンター
const getPenChecked = () => document.getElementById('trendLinePen').checked; // ペン（描画）モードのチェック状態を取得
const getEraserChecked = () => document.getElementById('trendLineEraser').checked; // 消しゴムモードのチェック状態を取得

let mouseDownHandler = null;
let mouseUpHandler = null;

export const enableTrendlineDrawing = (chart) => {
	const canvas = chart.canvas;
	const xScale = chart.scales.x;
	const yScale = chart.scales.y;

	// 既存のイベントリスナーを削除（存在すれば）chart.destoryしてもリスナー削除されないため
	if (mouseDownHandler) canvas.removeEventListener('mousedown', mouseDownHandler);
	if (mouseUpHandler) canvas.removeEventListener('mouseup', mouseUpHandler);

	// 左クリックの押下時処理（penの場合は線のstartポイントを作成、eraserの場合は線を削除）
	mouseDownHandler = (e) => {
		const rect = canvas.getBoundingClientRect();
		const mouseX = e.clientX - rect.left;
		const mouseY = e.clientY - rect.top;

		if (e.button !== 0) return; // 左クリックのみ処理

		if (getPenChecked()) {
			// ペンモード：線を描く
			start = {
				x: xScale.getValueForPixel(mouseX),
				y: yScale.getValueForPixel(mouseY)
			};
		} else if (getEraserChecked()) {
			// 消しゴムモード：線を削除
			const clickedLineId = findClickedLine(chart, mouseX, mouseY);
			if (clickedLineId) {
				delete trendlineAnnotations[clickedLineId]; // アノテーションから削除
				chart.update(); // グラフ更新
			}
		}
	};

	// 左クリックを離す時の処理（線のendポイントを決まり、線のデータを設定）
	mouseUpHandler = (e) => {
		if (!start || e.button !== 0) return;

		const rect = canvas.getBoundingClientRect();
		const mouseX = e.clientX - rect.left;
		const mouseY = e.clientY - rect.top;

		const end = {
			x: xScale.getValueForPixel(mouseX),
			y: yScale.getValueForPixel(mouseY)
		};

		// 一意なIDでトレンドラインを登録
		const id = `trendline-${lineId++}`;
		trendlineAnnotations[id] = {
			type: 'line',
			xMin: start.x,
			xMax: end.x,
			yMin: start.y,
			yMax: end.y,
			borderColor: chartStyleConfig.trendLineBorderColor,
			borderWidth: chartStyleConfig.trendLineBorderWidth,
			borderCapStyle: 'round',
		};

		chart.update(); // グラフ更新
		start = null; // 開始点リセット
	};

	// 新しいイベントリスナーを追加
	canvas.addEventListener('mousedown', mouseDownHandler);
	canvas.addEventListener('mouseup', mouseUpHandler);
};

// クリックされたラインを検出する関数
const findClickedLine = (chart, mouseX, mouseY) => {
	const xScale = chart.scales.x;
	const yScale = chart.scales.y;

	for (const lineId in trendlineAnnotations) {
		const line = trendlineAnnotations[lineId];
		const x1 = xScale.getPixelForValue(line.xMin);
		const x2 = xScale.getPixelForValue(line.xMax);
		const y1 = yScale.getPixelForValue(line.yMin);
		const y2 = yScale.getPixelForValue(line.yMax);

		// マウス位置とラインとの距離を計算
		const distance = distanceToLine(mouseX, mouseY, x1, y1, x2, y2);
		if (distance < 10) { // 10ピクセル以内ならヒットと判定
			return lineId;
		}
	}
	return null;
};

// 点と線分の距離を計算する関数
const distanceToLine = (px, py, x1, y1, x2, y2) => {
	const A = px - x1;
	const B = py - y1;
	const C = x2 - x1;
	const D = y2 - y1;

	const dot = A * C + B * D;
	const lenSq = C * C + D * D;
	let param = -1;
	if (lenSq !== 0) param = dot / lenSq;

	let xx, yy;
	if (param < 0) {
		xx = x1;
		yy = y1;
	} else if (param > 1) {
		xx = x2;
		yy = y2;
	} else {
		xx = x1 + param * C;
		yy = y1 + param * D;
	}

	const dx = px - xx;
	const dy = py - yy;
	return Math.sqrt(dx * dx + dy * dy);
};
