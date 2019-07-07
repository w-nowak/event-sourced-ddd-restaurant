package com.wnowakcraft.samples.restaurant.core.utils;

import java.time.*;

public class ApplicationClock {
	private static Clock currentClock = getSystemDefaultClock();

	private ApplicationClock() {
	}

	public static Clock getSystemDefaultClock() {
		return Clock.systemDefaultZone();
	}

	public static Clock getFixedClockFor(int year, int month, int dayOfMonth) {
		return Clock.fixed(
			LocalDate
				.of(year, month, dayOfMonth)
				.atStartOfDay(ZoneId.systemDefault())
				.toInstant(),
			ZoneId.systemDefault()
		);
	}

	public static Clock getFixedClockFor(int year, int month, int dayOfMonth, int hour, int minute, int second) {
		return Clock.fixed(
			ZonedDateTime.of(
				LocalDateTime.of(year, month, dayOfMonth, hour, minute, second),
				ZoneId.systemDefault()
			).toInstant(),
			ZoneId.systemDefault()
		);
	}

	public static Clock getCurrentClock() {
		return currentClock;
	}

	public static void setCurrentClock(Clock newClock) {
		currentClock = newClock;
	}
}
