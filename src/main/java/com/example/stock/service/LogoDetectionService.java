package com.example.stock.service;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.stock.dto.LogoSummaryWithTickers;
import com.example.stock.model.Tickers;
import com.example.stock.prompt.PromptLoader;
import com.example.stock.prompt.PromptType;
import com.example.stock.repository.TickersRepository;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
@RequiredArgsConstructor
public class LogoDetectionService {

	private final ImageAnnotatorClient visionClient;
	private final PromptLoader loader;
	private final OkHttpClient client;
	private final TickersRepository tickersRepository;

	@Value("${gemini.api.url}")
	private String geminiApiUrl;

	@Value("${gemini.api.key}")
	private String geminiApiKey;

	public void setGeminiApiUrl(String geminiApiUrl) {
		this.geminiApiUrl = geminiApiUrl;
	}

	public void setGeminiApiKey(String geminiApiKey) {
		this.geminiApiKey = geminiApiKey;
	}

	// ファイルアップロードのvalidation
	public String validateImageFile(MultipartFile file) {
		if (file.isEmpty()) {
			return "ファイルをアップロードしてください。";
		}

		String contentType = file.getContentType();
		if (contentType == null ||
				!(contentType.equals("image/jpeg") || contentType.equals("image/png"))) {
			return "JPEGまたはPNG形式の画像のみアップロード可能です。";
		}

		if (file.getSize() > 2 * 1024 * 1024) {
			return "ファイルサイズは2MB以内にしてください。";
		}

		return null;
	}

	// Vision APIを使って、画像から企業ロゴを検出し、信頼度付きのロゴ情報を返す
	public List<String> detectLogos(MultipartFile file) throws Exception {
		Map<String, Float> logoMap = new HashMap<>();

		// アップロードされた画像ファイルの中身をバイナリデータ（ByteString）として読み込む
		ByteString imgBytes = ByteString.readFrom(file.getInputStream());
		// Vision API に渡す画像オブジェクトを作成（内容は imgBytes）
		Image img = Image.newBuilder().setContent(imgBytes).build();
		// 検出したい機能として「ロゴ検出（LOGO_DETECTION）」を指定
		Feature feat = Feature.newBuilder().setType(Feature.Type.LOGO_DETECTION).build();
		// 画像と検出機能を組み合わせて、Vision API への画像解析リクエストを構築
		AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
				.addFeatures(feat)
				.setImage(img)
				.build();

		// Vision APIにリクエストを送信し、レスポンス（検出結果）を取得
		List<AnnotateImageResponse> responses = visionClient
				.batchAnnotateImages(Collections.singletonList(request))
				.getResponsesList();

		// 結果からロゴ情報を取り出してリストに追加
		for (AnnotateImageResponse res : responses) {
			if (res.hasError()) {
				// Vision APIのエラーハンドリング
				throw new RuntimeException("Vision API error:" + res.getError().getMessage());
			}

			List<EntityAnnotation> annotations = res.getLogoAnnotationsList();
			if (annotations.isEmpty()) {
				return List.of("ロゴが検出されませんでした");
			}
			for (EntityAnnotation annotation : res.getLogoAnnotationsList()) {
				String company = annotation.getDescription();
				double score = annotation.getScore();

				if (score >= 0.5f) {
					logoMap.merge(company, (float) score, Math::max);
				}
			}

		}

		if (logoMap.isEmpty()) {
			return List.of("ロゴが検出されませんでした");
		}

		return logoMap.entrySet().stream()
				.sorted((e1, e2) -> Float.compare(e2.getValue(), e1.getValue()))
				.map(e -> e.getKey() + "（信頼度：" + Math.round(e.getValue() * 100) + "％）")
				.collect(Collectors.toList());
	}

	public LogoSummaryWithTickers summarizeWithGemini(String visionJson) throws IOException {
		String url = geminiApiUrl + geminiApiKey;
		String prompt = loader.loadPrompt(PromptType.MARKDOWN, visionJson);
		String requestBody = loader.loadRequestJson(prompt);

		Request request = new Request.Builder()
				.url(url)
				.post(RequestBody.create(requestBody, MediaType.get("application/json")))
				.build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response);
			}
			String responseBody = response.body().string();
			JSONObject obj = new JSONObject(responseBody);
			String markdown = obj.getJSONArray("candidates")
					.getJSONObject(0)
					.getJSONObject("content")
					.getJSONArray("parts")
					.getJSONObject(0)
					.getString("text");

			String ticker = extractTicker(markdown);
			String brand = extractBrand(markdown);

			Tickers entity = getOrSaveTicker(ticker, brand);

			Parser parser = Parser.builder().build();
			HtmlRenderer renderer = HtmlRenderer.builder().build();
			String html = renderer.render(parser.parse(markdown));

			return new LogoSummaryWithTickers(entity, html);
		}
	}

	private String extractTicker(String markdown) {
		String[] lines = markdown.split("\n");
		for (int i = 0; i < lines.length - 1; i++) {
			if (lines[i].trim().contains("企業のTicker")) {
				return lines[i + 1].trim(); // 次の行がTicker
			}
		}
		return "";
	}

	private String extractBrand(String markdown) {
		String[] lines = markdown.split("\n");
		for (int i = 0; i < lines.length - 1; i++) {
			if (lines[i].trim().contains("企業名")) {
				String raw = lines[i + 1].trim();
				return raw.replaceAll("[（(].*?[）)]", "").trim();

			}
		}
		return "";
	}

	/**
	 * TickersエンティティをDBに保存（既存データがなければ）。
	 *
	 * @param ticker ティッカー（例: "KO"）
	 * @param brand ブランド名（例: "The Coca-Cola Company"）
	 */
	public Tickers getOrSaveTicker(String ticker, String brand) {
		if (ticker == null || ticker.isBlank() || brand == null || brand.isBlank()) {
			return null;
		}

		Tickers existing = tickersRepository.findByTicker(ticker);
		if (existing != null) {
			return existing;
		}

		Tickers newTicker = new Tickers();
		newTicker.setTicker(ticker);
		newTicker.setBrand(brand);
		tickersRepository.save(newTicker);
		return newTicker;
	}

}