package com.example.stock.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.stock.dto.LogoSummaryWithTickers;
import com.example.stock.service.LogoDetectionService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/logo")
@RequiredArgsConstructor
public class LogoDetectionController {
	private final LogoDetectionService logoDetectionService;

	@GetMapping("/detect")
	public String showForm() {
		return "logo/upload";
	}

	@PostMapping("/detect")
	public String detectLogo(@RequestParam("file") MultipartFile file, Model model) {
		List<String> logos = new ArrayList<>();
		String summaryHTML = "";
		LogoSummaryWithTickers logoSummaryWithTickers = null;

		String error = logoDetectionService.validateImageFile(file);
		if (error != null) {
			model.addAttribute("error", error);
			return "logo/upload";
		}

		try {
			logos = logoDetectionService.detectLogos(file);
			if (!logos.isEmpty() && !logos.get(0).equals("ロゴが検出されませんでした")) {
				String topLogo = logos.get(0);
				String companyName = topLogo.split("（")[0];
				logoSummaryWithTickers = logoDetectionService.summarizeWithGemini(companyName);
				summaryHTML = logoSummaryWithTickers.getSummaryHtml();
			}

		} catch (Exception e) {
			model.addAttribute("error", "ロゴ検出中にエラーが発生しました：" + e.getMessage());
		}

		model.addAttribute("logos", logos);
		model.addAttribute("summaryHTML", summaryHTML);

		//デフォルトをAAPLに設定
		String tickerSymbol = (logoSummaryWithTickers != null && logoSummaryWithTickers.getTickerEntity() != null)
				? logoSummaryWithTickers.getTickerEntity().getTicker()
				: "AAPL";
		model.addAttribute("symbol", tickerSymbol);
		return "logo/upload";
	}

}
