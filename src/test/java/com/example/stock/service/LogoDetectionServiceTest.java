package com.example.stock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.example.stock.prompt.PromptLoader;
import com.example.stock.prompt.PromptType;
import com.example.stock.repository.TickersRepository;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.rpc.Status;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@ExtendWith(MockitoExtension.class)
public class LogoDetectionServiceTest {

	@Mock
	private ImageAnnotatorClient mockClient;

	@Mock
	private PromptLoader mockLoader;

	@Mock
	private OkHttpClient mockHttpClient;

	@Mock
	private TickersRepository tickersRepository;

	@Mock
	private Call mockCall;

	private LogoDetectionService service;

	@BeforeEach
	void setUp() {
		service = new LogoDetectionService(mockClient, mockLoader, mockHttpClient, tickersRepository);
		service.setGeminiApiUrl("https://dummy.api/");
		service.setGeminiApiKey("dummy-key");
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
		// モックレスポンス作成（今回は中身不要）
		AnnotateImageResponse mockResponse = AnnotateImageResponse.newBuilder().build();
		BatchAnnotateImagesResponse response = BatchAnnotateImagesResponse.newBuilder()
				.addResponses(mockResponse)
				.build();
		when(mockClient.batchAnnotateImages(anyList())).thenReturn(response);

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

	// F-003-TC1 GeminiがMarkDownを返し、HTMLに変換(正常系)
	@Test
	void testSummarizeWithGemini_returnsHtml() throws Exception {
		// 入力データ
		String visionJson = "{\"company\": \"Google\"}";
		String prompt = "企業情報を以下から抽出してください: Google";
		String requestJson = "{ \"contents\": \"prompt\" }";

		// Geminiのレスポンス（Markdownテキスト含む）
		String markdown = "# Google\n企業概要です。";
		String fakeGeminiResponse = """
				{
				  "candidates": [
				    {
				      "content": {
				        "parts": [
				          {
				            "text": "# Google\\n企業概要です。"
				          }
				        ]
				      }
				    }
				  ]
				}
				""";

		// OkHttpのレスポンス構築
		Response mockResponse = new Response.Builder()
				.request(new Request.Builder().url("http://dummy").build())
				.protocol(Protocol.HTTP_1_1)
				.code(200)
				.message("OK")
				.body(ResponseBody.create(fakeGeminiResponse, MediaType.get("application/json")))
				.build();

		// モック設定
		when(mockLoader.loadPrompt(PromptType.MARKDOWN, visionJson)).thenReturn(prompt);
		when(mockLoader.loadRequestJson(prompt)).thenReturn(requestJson);
		when(mockHttpClient.newCall(any())).thenReturn(mockCall);
		when(mockCall.execute()).thenReturn(mockResponse);

		// 実行
		String result = service.summarizeWithGemini(visionJson).getSummaryHtml();

		// 検証：HTML変換されてるか
		assertTrue(result.contains("<h1>Google</h1>"));
		assertTrue(result.contains("企業概要です。"));
	}

	// F-003-TC2 GeminiがHTTPエラーの時にIOExceptionをスローする
	@Test
	void testSummarizeWithGemini_whenGeminiApiFails_throwsIOException() throws Exception {
		// モック設定
		String visionJson = "{\"company\": \"Google\"}";
		String prompt = "prompt";
		String requestJson = "{ \"contents\": \"prompt\" }";

		when(mockLoader.loadPrompt(PromptType.MARKDOWN, visionJson)).thenReturn(prompt);
		when(mockLoader.loadRequestJson(prompt)).thenReturn(requestJson);
		when(mockHttpClient.newCall(any())).thenReturn(mockCall);

		// HTTPエラー（400 Bad Request）を模擬
		Response errorResponse = new Response.Builder()
				.request(new Request.Builder().url("http://dummy").build())
				.protocol(Protocol.HTTP_1_1)
				.code(400)
				.message("Bad Request")
				.body(ResponseBody.create("error", MediaType.get("application/json")))
				.build();

		when(mockCall.execute()).thenReturn(errorResponse);

		// 実行＆検証
		assertThrows(IOException.class, () -> service.summarizeWithGemini(visionJson));
	}
}
