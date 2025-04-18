package com.example.stock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.google.cloud.vision.v1.ImageAnnotatorClient;

@SpringBootTest
class StockApplicationTests {
	@MockBean
	private ImageAnnotatorClient visionClient;

	@Test
	void contextLoads() {
	}

}
