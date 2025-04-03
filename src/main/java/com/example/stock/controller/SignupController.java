package com.example.stock.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.stock.exception.UserRegistrationException;
import com.example.stock.model.Users;
import com.example.stock.service.UsersService;

@Controller
public class SignupController {
	private final UsersService usersService;

	public SignupController(UsersService usersService) {
		this.usersService = usersService;
	}

	@GetMapping("/signup")
	public String showSignupForm(Model model) {
		model.addAttribute("users", new Users());
		return "signup";
	}

	@PostMapping("/signup")
	public String processSignup(@ModelAttribute @Valid Users users, BindingResult result,
			RedirectAttributes redirectAttributes, Model model) {

		if (result.hasErrors()) {
			return "signup";
		}

		try {
			// serviceで処理を行う
			usersService.registerUser(users);

			// 成功メッセージをフラッシュ属性で送る
			redirectAttributes.addFlashAttribute("success", "仮登録が完了しました。認証メールをご確認ください。");

			// ログインページにリダイレクト
			return "redirect:/login";

		} catch (UserRegistrationException e) {
			result.rejectValue(e.getFieldName(), "error.users", e.getMessage());
			return "signup";
		}

	}
}