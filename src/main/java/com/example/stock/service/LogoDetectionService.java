package com.example.stock.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogoDetectionService {

	private final ImageAnnotatorClient visionClient;

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
		List<String> logos = new ArrayList<>();

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
			for (EntityAnnotation annotation : res.getLogoAnnotationsList()) {
				String company = annotation.getDescription();
				double score = annotation.getScore();
				logos.add(company + "（信頼度：" + String.format("%.2f", score) + "）");
			}
		}

		return logos;
	}
}
