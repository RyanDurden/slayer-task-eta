package com.slayereta;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Works out a kill rate, and how much time goes on breaks, from the times
 * task kills are seen.
 *
 * Time only counts towards the kill rate between two kills that are close
 * together. A longer gap (banking, walking back, AFK) is a pause: it is left
 * out of the rate, and so is the kill that ended it. If the pause was short
 * enough to be a break rather than the player being away, its length is kept
 * so the ETA can allow for it.
 *
 * Every counted gap is kept, so the rate can be averaged over the whole task or
 * over only the most recent kills.
 */
class KillRateTracker
{
	private static final double MS_PER_HOUR = 3_600_000d;

	/** A pause longer than this is the player being away, not a break, and is ignored. */
	static final long MAX_BREAK_MS = 15 * 60_000L;

	/** With nothing saved, this much killing is needed before the break estimate is used. */
	static final long MIN_KILLING_MS_FOR_BREAKS = 10 * 60_000L;

	/** A saved break estimate counts as if it came from this much killing. */
	static final long SAVED_BREAKS_WEIGHT_MS = 30 * 60_000L;

	/** One counted gap: {@code kills} kills landed {@code ms} after the previous kill. */
	private static final class Gap
	{
		final long ms;
		final int kills;

		Gap(long ms, int kills)
		{
			this.ms = ms;
			this.kills = kills;
		}
	}

	private final Deque<Gap> gaps = new ArrayDeque<>();
	private long lastKillMs = -1;
	private long killingMs;
	private int countedKills;
	private long breakMs;

	/**
	 * Record that {@code count} kills happened at {@code nowMs}.
	 *
	 * @param pauseMs a gap longer than this is a pause rather than killing time
	 */
	void recordKills(int count, long nowMs, long pauseMs)
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
		if (gap > 0 && gap <= pauseMs)
		{
			gaps.addLast(new Gap(gap, count));
			killingMs += gap;
			countedKills += count;
		}
		else if (gap > pauseMs && gap <= MAX_BREAK_MS)
		{
			// A pause: left out of the rate, along with the kill that ended it,
			// but remembered as a break.
			breakMs += gap;
		}
		lastKillMs = nowMs;
	}

	/**
	 * Restart the clock at {@code nowMs} without counting anything, not even as
	 * a break. Used after a superior kill, so the time spent on it stays out of
	 * both the rate and the breaks.
	 */
	void restartClock(long nowMs)
	{
		lastKillMs = nowMs;
	}

	/**
	 * Forget when the last kill was, so the next kill only starts the clock.
	 * Used on logout, so time spent logged out is never counted as a break.
	 */
	void clearClock()
	{
		lastKillMs = -1;
	}

	/**
	 * Kills counted towards the whole-task rate (excludes clock-starting kills).
	 */
	int getCountedKills()
	{
		return countedKills;
	}

	/** Time spent killing, in ms: the counted gaps added up. */
	long getKillingMs()
	{
		return killingMs;
	}

	/** Time spent on breaks, in ms. */
	long getBreakMs()
	{
		return breakMs;
	}

	/**
	 * Kills per hour over every counted kill, or 0 when there isn't enough data.
	 */
	double getKillsPerHour()
	{
		if (countedKills <= 0 || killingMs <= 0)
		{
			return 0;
		}
		return countedKills * MS_PER_HOUR / killingMs;
	}

	/**
	 * Kills per hour over roughly the last {@code lastKills} counted kills, or 0
	 * when there isn't enough data.
	 */
	double getKillsPerHour(int lastKills)
	{
		long ms = 0;
		int kills = 0;
		Iterator<Gap> it = gaps.descendingIterator();
		while (it.hasNext() && kills < lastKills)
		{
			Gap g = it.next();
			ms += g.ms;
			kills += g.kills;
		}
		if (kills <= 0 || ms <= 0)
		{
			return 0;
		}
		return kills * MS_PER_HOUR / ms;
	}

	/**
	 * Break time per unit of killing time (0.25 means 15 minutes of breaks for
	 * every hour of killing), or -1 when there is no estimate yet.
	 *
	 * @param savedRatio the estimate saved from earlier sessions, or a negative number for none
	 */
	double getBreakRatio(double savedRatio)
	{
		return breakRatio(savedRatio, killingMs, breakMs);
	}

	/**
	 * Combine a saved break estimate with what has been seen this session.
	 *
	 * With a saved estimate, it counts as {@link #SAVED_BREAKS_WEIGHT_MS} of past
	 * killing and this session's numbers are added on top, so one odd bank trip
	 * only nudges it and a long session gradually takes over. With nothing
	 * saved, this session alone is used once it has enough killing in it.
	 *
	 * @return the ratio, or -1 for no estimate
	 */
	static double breakRatio(double savedRatio, long killingMs, long breakMs)
	{
		if (savedRatio >= 0)
		{
			return (savedRatio * SAVED_BREAKS_WEIGHT_MS + breakMs) / (SAVED_BREAKS_WEIGHT_MS + killingMs);
		}
		if (killingMs >= MIN_KILLING_MS_FOR_BREAKS)
		{
			return (double) breakMs / killingMs;
		}
		return -1;
	}

	void reset()
	{
		gaps.clear();
		lastKillMs = -1;
		killingMs = 0;
		countedKills = 0;
		breakMs = 0;
	}
}
