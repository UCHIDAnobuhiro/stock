package com.example.stock.exception;

public class UserRegistrationException extends RuntimeException {
	private final String fieldName;

	public UserRegistrationException(String fieldName, String message) {
		super(message);
		this.fieldName = fieldName;
	}

	public String getFieldName() {
		return fieldName;
	}
}