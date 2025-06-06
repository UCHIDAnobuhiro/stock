package com.example.stock.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.stock.dto.ResetPasswordForm;
import com.example.stock.enums.TokenType;
import com.example.stock.exception.UserRegistrationException;
import com.example.stock.service.UsersService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ResetPasswordController {
	private final UsersService usersService;

	@GetMapping("/password/request")
	public String showPasswordRequest() {
		return "password/request-form";
	}

	@PostMapping("/password/email")
	public String resetPassword(@RequestParam("email") String email, @RequestParam("type") TokenType tokenType,
			RedirectAttributes redirectAttributes) {
		try {
			// serviceで処理を行う
			usersService.resendVerificationEmail(email, tokenType);

			// 成功メッセージをフラッシュ属性で送る
			redirectAttributes.addFlashAttribute("success", "パスワード再設定ページのリンクを送信しました");

			// ログインページにリダイレクト
			return "redirect:/login";

		} catch (UserRegistrationException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/password/request";
		}
	}

	@GetMapping("/password/reset")
	public String verifyPasswordUser(@RequestParam("token") String token,
			RedirectAttributes redirectAttributes, Model model) {
		// serviceでtokenの有効性を確認
		boolean isValid = usersService.validateResetPasswordToken(token);

		if (!isValid) {
			redirectAttributes.addFlashAttribute("error", "トークンが無効または期限切れです。");
			return "redirect:/login";
		}

		model.addAttribute("form", new ResetPasswordForm()); // ← formバインディング用
		model.addAttribute("token", token); // ← hiddenで使う
		// 有効なトークンなのでリセット画面へ遷移
		return "password/reset";

	}

	@PostMapping("/password/reset")
	public String handlePasswordReset(
			@RequestParam("token") String token,
			@ModelAttribute("form") @Valid ResetPasswordForm form,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes,
			Model model) {
		// フィールドの基本バリデーション（@NotBlankなど）
		if (bindingResult.hasErrors()) {
			model.addAttribute("token", token); // hidden用
			return "password/reset";
		}

		// パスワード一致チェック
		if (!form.getPassword().equals(form.getConfirmPassword())) {
			bindingResult.rejectValue("confirmPassword", "password.mismatch", "パスワードが一致しません");
			model.addAttribute("token", token);
			return "password/reset";
		}

		// パスワード更新処理
		boolean success = usersService.resetPassword(token, form.getPassword());

		if (success) {
			redirectAttributes.addFlashAttribute("success", "パスワードを変更しました。ログインしてください。");
			return "redirect:/login";
		} else {
			redirectAttributes.addFlashAttribute("error", "トークンが無効または期限切れです。");
			return "redirect:/password/request";
		}

	}

}
