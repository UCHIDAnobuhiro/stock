package com.example.stock.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordForm {
	@NotBlank(message = "パスワードを入力してください")
	private String password;

	@NotBlank(message = "確認用パスワードを入力してください")
	private String confirmPassword;

}
