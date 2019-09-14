package com.wnowakcraft.samples.restaurant.core.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ApplicationTime {

	private ApplicationTime() {
	}

	public static LocalDate dateNow() {
		return LocalDate.now(ApplicationClock.getCurrentClock());
	}

	public static LocalDateTime dateTimeNow() {
		return LocalDateTime.now(ApplicationClock.getCurrentClock());
	}

	public static Instant instantNow() {
		return Instant.now(ApplicationClock.getCurrentClock());
	}
}
