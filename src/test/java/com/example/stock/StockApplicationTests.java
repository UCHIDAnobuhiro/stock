package com.example.stock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.example.stock.prompt.PromptLoader;
import com.google.cloud.vision.v1.ImageAnnotatorClient;

@SpringBootTest
class StockApplicationTests {
	@MockBean
	private ImageAnnotatorClient visionClient;

	@MockBean
	private PromptLoader promptLoader;

	@Test
	void contextLoads() {
	}

}
