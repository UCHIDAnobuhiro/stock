package com.example.stock.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.stock.exception.UserRegistrationException;
import com.example.stock.service.UsersService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class VerificationController {
	private final UsersService usersService;

	@GetMapping("/verify")
	public String verifyUser(@RequestParam("token") String token, RedirectAttributes redirectAttributes) {
		boolean verified = usersService.verifyUser(token);

		if (verified) {
			redirectAttributes.addFlashAttribute("success", "アカウントが有効化されました。ログインしてください。");
		} else {
			redirectAttributes.addFlashAttribute("error", "トークンが無効または期限切れです。");
		}

		return "redirect:/login";
	}

	@GetMapping("/resend-form")
	public String showResendForm() {
		return "resend-form";
	}

	@PostMapping("/resend-verification")
	public String resendVerification(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
		try {
			// serviceで処理を行う
			usersService.resendVerificationEmail(email);

			// 成功メッセージをフラッシュ属性で送る
			redirectAttributes.addFlashAttribute("success", "確認メールを再送信しました");

			// ログインページにリダイレクト
			return "redirect:/login";

		} catch (UserRegistrationException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/resend-form";
		}
	}
}
