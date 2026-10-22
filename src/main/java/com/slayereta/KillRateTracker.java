package com.slayereta;

/**
 * Works out a kill rate from the times task kills are seen.
 *
 * Time only counts between two kills that are close together. Any gap longer
 * than the break threshold (banking, AFK, walking back) is dropped, so the rate
 * reflects how fast you kill while actually on task, not your downtime.
 */
class KillRateTracker
{
	private static final double MS_PER_HOUR = 3_600_000d;

	private long lastKillMs = -1;
	private long activeMs;
	private int kills;

	/**
	 * Record that {@code count} kills happened at {@code nowMs}.
	 */
	void recordKills(int count, long nowMs, long breakThresholdMs)
	{
		if (count <= 0)
		{
			return;
		}

		if (lastKillMs < 0)
		{
			// The first kill seen only starts the clock. There is no earlier
			// timestamp to measure it against, so it cannot be counted.
			lastKillMs = nowMs;
			return;
		}

		long gap = nowMs - lastKillMs;
		if (gap > 0 && gap <= breakThresholdMs)
		{
			activeMs += gap;
			kills += count;
		}
		// A gap over the threshold is a break: skip that stretch of time and
		// the kill that ended it, and restart the clock from here.
		lastKillMs = nowMs;
	}

	/**
	 * Kills counted towards the rate (excludes clock-starting kills).
	 */
	int getCountedKills()
	{
		return kills;
	}

	/**
	 * Kills per hour, or 0 when there isn't enough data yet.
	 */
	double getKillsPerHour()
	{
		if (kills <= 0 || activeMs <= 0)
		{
			return 0;
		}
		return kills * MS_PER_HOUR / activeMs;
	}

	void reset()
	{
		lastKillMs = -1;
		activeMs = 0;
		kills = 0;
	}
}
