package com.wnowakcraft.samples.restaurant.core.utils;

import java.time.*;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;

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
		return prepareClockFor(
			ZonedDateTime.of(
				LocalDateTime.of(year, month, dayOfMonth, hour, minute, second),
				ZoneId.systemDefault()
			).toInstant()
		);
	}

	public static Clock getFixedClockFor(Instant instant) {
		requireNonNull(instant, "instant");
		return prepareClockFor(instant);
	}

	private static Clock prepareClockFor(Instant instant) {
		return  Clock.fixed(
				instant,
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
