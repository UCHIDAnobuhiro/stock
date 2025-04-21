package com.example.stock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.ImageAnnotatorClient;

public class LogoDetectionServiceTest {

	private final ImageAnnotatorClient mockClient = mock(ImageAnnotatorClient.class); // ★追加
	private final LogoDetectionService service = new LogoDetectionService(mockClient);

	// TC1: ファイル未選択
	@Test
	void testValidateFile_emptyFile_returnsError() {
		MultipartFile mockFile = mock(MultipartFile.class);
		when(mockFile.isEmpty()).thenReturn(true);

		String result = service.validateImageFile(mockFile);

		assertEquals("ファイルをアップロードしてください。", result);
	}

	// TC2: 不正なコンテンツタイプ(PDF)
	@Test
	void testValidateFile_invalidContentType_returnsError() {
		MultipartFile mockFile = mock(MultipartFile.class);
		when(mockFile.isEmpty()).thenReturn(false);
		when(mockFile.getContentType()).thenReturn("application/pdf");

		String result = service.validateImageFile(mockFile);

		assertEquals("JPEGまたはPNG形式の画像のみアップロード可能です。", result);
	}

	// TC3: コンテンツタイプがnull
	@Test
	void testValidateFile_nullContentType_returnsError() {
		MultipartFile mockFile = mock(MultipartFile.class);
		when(mockFile.isEmpty()).thenReturn(false);
		when(mockFile.getContentType()).thenReturn("null");

		String result = service.validateImageFile(mockFile);

		assertEquals("JPEGまたはPNG形式の画像のみアップロード可能です。", result);
	}

	// TC4: ファイルサイズが2MB超過
	@Test
	void testValidateFile_oversizedFile_returnsError() {
		MultipartFile mockFile = mock(MultipartFile.class);
		when(mockFile.isEmpty()).thenReturn(false);
		when(mockFile.getContentType()).thenReturn("image/jpeg");
		when(mockFile.getSize()).thenReturn(3 * 1024 * 1024L); // 3MB

		String result = service.validateImageFile(mockFile);

		assertEquals("ファイルサイズは2MB以内にしてください。", result);
	}

	// TC5: JPEGで正常
	@Test
	void testValidateFile_validJpeg_returnsNull() {
		MultipartFile mockFile = mock(MultipartFile.class);
		when(mockFile.isEmpty()).thenReturn(false);
		when(mockFile.getContentType()).thenReturn("image/jpeg");
		when(mockFile.getSize()).thenReturn(1 * 1024 * 1024L); // 1MB

		String result = service.validateImageFile(mockFile);

		assertNull(result);
	}

	// TC6: PNGで正常
	@Test
	void testValidateFile_validPng_returnsNull() {
		MultipartFile mockFile = mock(MultipartFile.class);
		when(mockFile.isEmpty()).thenReturn(false);
		when(mockFile.getContentType()).thenReturn("image/png");
		when(mockFile.getSize()).thenReturn(512 * 1024L); // 512KB

		String result = service.validateImageFile(mockFile);

		assertNull(result);
	}

	// TC7: GCVにリクエストを送る
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

}
