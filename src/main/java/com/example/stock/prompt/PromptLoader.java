package com.example.stock.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class PromptLoader {
	public String loadPrompt(PromptType type, String visionJson) throws IOException {
		String filename = switch (type) {
		case MARKDOWN -> "prompts/summary_prompt.md";
		case JSON -> "prompts/summary_prompt_json.md";
		};
		return loadPromptTemplate(filename, visionJson);
	}

	private String loadPromptTemplate(String templatePath, String visionJson) throws IOException {
		try (InputStream is = getClass().getClassLoader().getResourceAsStream(templatePath)) {
			if (is == null) {
				throw new IOException("テンプレートが見つかりません: " + templatePath);
			}
			String rawTemplate = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			return rawTemplate.replace("{{visionJson}}", visionJson);
		}
	}

	public String loadRequestJson(String prompt) throws IOException {
		try (InputStream is = getClass().getClassLoader().getResourceAsStream("prompts/gemini_request_template.json")) {
			if (is == null) {
				throw new IOException("テンプレートが見つかりません");
			}
			String rawJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			return rawJson.replace("{{PROMPT}}", prompt);
		}
	}

}
