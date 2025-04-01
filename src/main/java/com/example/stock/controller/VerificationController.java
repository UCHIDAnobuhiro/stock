package com.example.stock.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
}
