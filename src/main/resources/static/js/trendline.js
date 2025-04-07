// trendline.js
export const trendlineAnnotations = {}; // すべてのトレンドラインを保存するオブジェクト

let start = null; // 描画開始点
let lineId = 0; // ラインIDカウンター

export const enableTrendlineDrawing = (chart) => {
  const canvas = chart.canvas;
  const xScale = chart.scales.x;
  const yScale = chart.scales.y;

  // 左ドラッグで線を描画開始
  canvas.addEventListener('mousedown', (e) => {
    if (e.button !== 0) return; // 左クリックのみ処理

    // マウス座標を取得
    const rect = canvas.getBoundingClientRect();
    const mouseX = e.clientX - rect.left;
    const mouseY = e.clientY - rect.top;

    // データ座標に変換して開始点を記録
    start = {
      x: xScale.getValueForPixel(mouseX),
      y: yScale.getValueForPixel(mouseY)
    };
  });

  // 左クリックを離して線描画完了
  canvas.addEventListener('mouseup', (e) => {
    if (!start || e.button !== 0) return; // 開始点がない場合や右クリックは無視

    // 終了点座標を取得
    const rect = canvas.getBoundingClientRect();
    const mouseX = e.clientX - rect.left;
    const mouseY = e.clientY - rect.top;

    const end = {
      x: xScale.getValueForPixel(mouseX),
      y: yScale.getValueForPixel(mouseY)
    };

    // ラインIDを生成し、アノテーションを追加
    const id = `trendline-${lineId++}`;
    trendlineAnnotations[id] = {
      type: 'line',
      xMin: start.x,
      xMax: end.x,
      yMin: start.y,
      yMax: end.y,
      borderColor: 'rgba(0, 150, 136, 0.8)',
      borderWidth: 2,
      borderCapStyle: 'round',
    };

    // チャートを更新し、開始点をリセット
    chart.update();
    start = null;
  });

  // 右クリックでライン削除
  canvas.addEventListener('contextmenu', (e) => {
    e.preventDefault(); // コンテキストメニューを防止

    // クリック位置を取得
    const rect = canvas.getBoundingClientRect();
    const mouseX = e.clientX - rect.left;
    const mouseY = e.clientY - rect.top;

    // クリックされたラインを検索
    const clickedLineId = findClickedLine(chart, mouseX, mouseY);
    if (clickedLineId) {
      delete trendlineAnnotations[clickedLineId]; // ラインを削除
      chart.update(); // チャートを更新
    }
  });
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

    // マウス位置とラインの距離を計算
    const distance = distanceToLine(mouseX, mouseY, x1, y1, x2, y2);
    if (distance < 10) { // 10ピクセル以内ならクリックと判定
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