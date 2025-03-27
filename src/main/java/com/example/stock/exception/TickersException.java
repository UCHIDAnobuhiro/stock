package com.example.stock.exception;

public class TickersException extends RuntimeException {
	private final String fieldName;

	public TickersException(String fieldName, String message) {
		super(message);
		this.fieldName = fieldName;
	}

	public String getFieldName() {
		return fieldName;
	}

	@Override
	public String getMessage() {
		return "Field '" + fieldName + "' caused an error: " + super.getMessage();
	}

}