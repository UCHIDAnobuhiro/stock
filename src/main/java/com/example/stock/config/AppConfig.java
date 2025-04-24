package com.example.stock.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import com.google.cloud.vision.v1.ImageAnnotatorClient;

import okhttp3.OkHttpClient;

@Configuration
public class AppConfig {

	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	@Profile("!test")
	ImageAnnotatorClient imageAnnotatorClient() throws IOException {
		return ImageAnnotatorClient.create();
	}

	@Bean
	OkHttpClient okHttpClient() {
		return new OkHttpClient();
	}
}
