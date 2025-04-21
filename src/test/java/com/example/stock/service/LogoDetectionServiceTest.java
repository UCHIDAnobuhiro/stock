package com.example.stock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.rpc.Status;

@ExtendWith(MockitoExtension.class)
public class LogoDetectionServiceTest {

	@Mock
	private ImageAnnotatorClient mockClient = mock(ImageAnnotatorClient.class);
	private LogoDetectionService service = new LogoDetectionService(mockClient);

	@BeforeEach
	void setUp() {
		service = new LogoDetectionService(mockClient);
	}

	// F-001-TC1: ファイル未選択
	@Test
	void testValidateFile_emptyFile_returnsError() {
		MultipartFile mockFile = mock(MultipartFile.class);
		when(mockFile.isEmpty()).thenReturn(true);

		String result = service.validateImageFile(mockFile);

		assertEquals("ファイルをアップロードしてください。", result);
	}

	// F-001-TC2: 不正なコンテンツタイプ(PDF)
	@Test
	void testValidateFile_invalidContentType_returnsError() {
		MultipartFile mockFile = mock(MultipartFile.class);
		when(mockFile.isEmpty()).thenReturn(false);
		when(mockFile.getContentType()).thenReturn("application/pdf");

		String result = service.validateImageFile(mockFile);

		assertEquals("JPEGまたはPNG形式の画像のみアップロード可能です。", result);
	}

	// F-001-TC3: コンテンツタイプがnull
	@Test
	void testValidateFile_nullContentType_returnsError() {
		MultipartFile mockFile = mock(MultipartFile.class);
		when(mockFile.isEmpty()).thenReturn(false);
		when(mockFile.getContentType()).thenReturn("null");

		String result = service.validateImageFile(mockFile);

		assertEquals("JPEGまたはPNG形式の画像のみアップロード可能です。", result);
	}

	// F-001-TC4: ファイルサイズが2MB超過
	@Test
	void testValidateFile_oversizedFile_returnsError() {
		MultipartFile mockFile = mock(MultipartFile.class);
		when(mockFile.isEmpty()).thenReturn(false);
		when(mockFile.getContentType()).thenReturn("image/jpeg");
		when(mockFile.getSize()).thenReturn(3 * 1024 * 1024L); // 3MB

		String result = service.validateImageFile(mockFile);

		assertEquals("ファイルサイズは2MB以内にしてください。", result);
	}

	// F-001-TC5: JPEGで正常
	@Test
	void testValidateFile_validJpeg_returnsNull() {
		MultipartFile mockFile = mock(MultipartFile.class);
		when(mockFile.isEmpty()).thenReturn(false);
		when(mockFile.getContentType()).thenReturn("image/jpeg");
		when(mockFile.getSize()).thenReturn(1 * 1024 * 1024L); // 1MB

		String result = service.validateImageFile(mockFile);

		assertNull(result);
	}

	// F-001-TC6: PNGで正常
	@Test
	void testValidateFile_validPng_returnsNull() {
		MultipartFile mockFile = mock(MultipartFile.class);
		when(mockFile.isEmpty()).thenReturn(false);
		when(mockFile.getContentType()).thenReturn("image/png");
		when(mockFile.getSize()).thenReturn(512 * 1024L); // 512KB

		String result = service.validateImageFile(mockFile);

		assertNull(result);
	}

	// F-001-TC7: GCVにリクエストを送る
	@Test
	void testDetectLogos_sendsRequestToVisionApi() throws Exception {
		// モックの作成
		ImageAnnotatorClient mockClient = mock(ImageAnnotatorClient.class);

		// モックレスポンス作成（今回は中身不要）
		AnnotateImageResponse mockResponse = AnnotateImageResponse.newBuilder().build();
		BatchAnnotateImagesResponse response = BatchAnnotateImagesResponse.newBuilder()
				.addResponses(mockResponse)
				.build();
		when(mockClient.batchAnnotateImages(anyList())).thenReturn(response);

		// サービス作成
		LogoDetectionService service = new LogoDetectionService(mockClient);

		// モックファイル作成
		byte[] dummyBytes = new byte[] { 1, 2, 3 };
		MultipartFile mockFile = mock(MultipartFile.class);
		when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(dummyBytes));

		// 実行
		service.detectLogos(mockFile);

		// Vision APIの送信が1回行われたかを確認
		verify(mockClient, times(1)).batchAnnotateImages(anyList());
	}

	// F-002-TC1 正常動作
	@Test
	void testDetectLogos_withValidLogo_returnsFormattedResult() throws Exception {
		// モック画像ファイルを作成
		byte[] dummyBytes = new byte[] { 1, 2, 3 };
		MultipartFile mockFile = mock(MultipartFile.class);
		when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(dummyBytes));

		// モックレスポンス作成
		EntityAnnotation logo = EntityAnnotation.newBuilder()
				.setDescription("Google")
				.setScore(0.95f)
				.build();

		AnnotateImageResponse response = AnnotateImageResponse.newBuilder()
				.addLogoAnnotations(logo)
				.build();

		BatchAnnotateImagesResponse batchResponse = BatchAnnotateImagesResponse.newBuilder()
				.addResponses(response)
				.build();

		// クライアントが返すように設定
		when(mockClient.batchAnnotateImages(anyList())).thenReturn(batchResponse);

		// 実行
		List<String> result = service.detectLogos(mockFile);

		// 検証
		assertEquals(1, result.size());
		assertEquals("Google（信頼度：95％）", result.get(0));
	}

	// F-002-TC2 信頼度が0.5未満のロゴ
	@Test
	void testDetectLogos_withLowScoreLogo_returnsEmptyMessage() throws Exception {
		MultipartFile mockFile = mock(MultipartFile.class);
		when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {}));

		EntityAnnotation logo = EntityAnnotation.newBuilder()
				.setDescription("Unknown")
				.setScore(0.3f) // ← 0.5未満なので除外される
				.build();

		AnnotateImageResponse response = AnnotateImageResponse.newBuilder()
				.addLogoAnnotations(logo)
				.build();

		BatchAnnotateImagesResponse batchResponse = BatchAnnotateImagesResponse.newBuilder()
				.addResponses(response)
				.build();

		when(mockClient.batchAnnotateImages(anyList())).thenReturn(batchResponse);

		List<String> result = service.detectLogos(mockFile);

		// 「ロゴが検出されませんでした」が返る想定
		assertEquals(1, result.size());
		assertEquals("ロゴが検出されませんでした", result.get(0));
	}

	// F-002-TC3 vision APIがエラーの時
	@Test
	void testDetectLogos_withApiError_throwsException() throws Exception {
		MultipartFile mockFile = mock(MultipartFile.class);
		when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {}));

		AnnotateImageResponse errorResponse = AnnotateImageResponse.newBuilder()
				.setError(Status.newBuilder().setMessage("API failure").build())
				.build();

		BatchAnnotateImagesResponse batchResponse = BatchAnnotateImagesResponse.newBuilder()
				.addResponses(errorResponse)
				.build();

		when(mockClient.batchAnnotateImages(anyList())).thenReturn(batchResponse);

		assertThrows(RuntimeException.class, () -> service.detectLogos(mockFile));
	}

}
