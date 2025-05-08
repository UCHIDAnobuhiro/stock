package com.example.stock.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.stock.service.OtpService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthController {
	private final OtpService otpService;

	@GetMapping("/otp")
	public String otpPage(Authentication authentication, Model model) {
		String email = authentication.getName(); // ログイン済みならここでemail取れる
		model.addAttribute("email", email);
		return "otp"; // otp.htmlを返す
	}

	@PostMapping("/verify-otp")
	public String verifyOtp(@RequestParam String otp,
			Authentication authentication,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		String email = authentication.getName();

		boolean valid = otpService.verifyOtp(email, otp);

		if (valid) {
			session.setAttribute("otpVerified", true);
			return "redirect:/logo/detect";
		} else {
			redirectAttributes.addFlashAttribute("error", "認証コードが正しくありません");
			return "redirect:/otp";
		}
	}

}