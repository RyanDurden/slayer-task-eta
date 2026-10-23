package com.slayereta;

import java.util.Locale;

/** Small calculations and formatting for the overlay, kept free of RuneLite types. */
final class EtaMath
{
	private EtaMath()
	{
	}

	/**
	 * Seconds until the task is finished, or -1 when there is no kill rate yet.
	 *
	 * @param breakRatio break time per unit of killing time; negative means leave breaks out
	 */
	static long etaSeconds(int remaining, double killsPerHour, double breakRatio)
	{
		if (killsPerHour <= 0)
		{
			return -1;
		}
		double killing = Math.max(0, remaining) * 3600d / killsPerHour;
		return Math.round(killing * (1 + Math.max(0, breakRatio)));
	}

	/**
	 * How long the overlay stays up after the last task kill or check, in ms,
	 * or 0 for never. It never goes below the pause time, so the overlay can't
	 * disappear while a slow kill is still in progress.
	 */
	static long hideDelayMs(int hideAfterMinutes, int pauseSeconds)
	{
		if (hideAfterMinutes <= 0)
		{
			return 0;
		}
		return Math.max(hideAfterMinutes * 60_000L, pauseSeconds * 1000L);
	}

	/** Lengths of time for the ETA and time on task, in the chosen format. */
	static String formatEta(long totalSeconds, EtaFormat format)
	{
		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;

		if (format == EtaFormat.DIGITAL)
		{
			return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
		}
		if (hours > 0)
		{
			return hours + "h " + minutes + "m";
		}
		if (minutes > 0)
		{
			return minutes + "m " + seconds + "s";
		}
		return seconds + "s";
	}
}
