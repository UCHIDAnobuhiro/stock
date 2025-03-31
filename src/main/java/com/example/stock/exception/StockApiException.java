package com.example.stock.exception;

public class StockApiException extends RuntimeException {
	public StockApiException(String message, Throwable cause) {
		super(message, cause);
	}

	public StockApiException(String message) {
		super(message);
	}
}
