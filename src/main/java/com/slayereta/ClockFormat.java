package com.slayereta;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** How the time of day the task should finish is written. */
public enum ClockFormat
{
	TWENTY_FOUR_HOUR("24-hour", DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)),
	TWELVE_HOUR("12-hour", DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH));

	private final String name;
	private final DateTimeFormatter formatter;

	ClockFormat(String name, DateTimeFormatter formatter)
	{
		this.name = name;
		this.formatter = formatter;
	}

	DateTimeFormatter getFormatter()
	{
		return formatter;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
