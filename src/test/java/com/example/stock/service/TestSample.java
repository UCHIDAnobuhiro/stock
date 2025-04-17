package com.example.stock.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TestSample {
	@Test
	void testAddition() {
		int a = 2;
		int b = 3;
		assertEquals(5, a + b);
	}

	@Test
	void testString() {
		String msg = "hello";
		assertTrue(msg.startsWith("he"));
	}

}
