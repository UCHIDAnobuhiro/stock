package com.example.stock.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	private final OtpCheckInterceptor otpCheckInterceptor;

	public WebConfig(OtpCheckInterceptor otpCheckInterceptor) {
		this.otpCheckInterceptor = otpCheckInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(otpCheckInterceptor)
				.addPathPatterns("/**")
				.excludePathPatterns("/login", "/otp", "/verify-otp", "/css/**", "/js/**");
	}

}
