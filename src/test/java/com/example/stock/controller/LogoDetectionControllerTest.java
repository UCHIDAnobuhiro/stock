package com.example.stock.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.example.stock.service.LogoDetectionService;

@WebMvcTest(LogoDetectionController.class)
@AutoConfigureMockMvc(addFilters = false)
public class LogoDetectionControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private LogoDetectionService logoDetectionService;

	@BeforeEach
	void setup() {

	}

	// F-001-TC8: GETリクエストフォームが表示される
	@Test
	void testShowForm_returnsUploadPage() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/logo/detect")
				.with(user("testuser").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(view().name("logo/upload"));
	}

	// F-001-TC9: エラーメッセージの表示
	@Test
	void testDetect_withUnsupportedFileType_setsMimeTypeErrorMessag() throws Exception {
		MockMultipartFile emptyFile = new MockMultipartFile("file", "test.pdf", "application/pdf",
				new byte[] { 1, 2, 3 });

		// モックの振る舞いを定義
		given(logoDetectionService.validateImageFile(emptyFile))
				.willReturn("JPEGまたはPNG形式の画像のみアップロード可能です。");

		mockMvc.perform(MockMvcRequestBuilders.multipart("/logo/detect")
				.file(emptyFile)
				.with(csrf())
				.with(user("testuser").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(model().attribute("error", "JPEGまたはPNG形式の画像のみアップロード可能です。"))
				.andExpect(view().name("logo/upload"));
	}
}
