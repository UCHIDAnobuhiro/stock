package com.example.stock.util;

public class OtpUtil {
	public static String generateOtp() {
		int otp = (int) (Math.random() * 900000) + 100000;
		return String.valueOf(otp);
	}
}
