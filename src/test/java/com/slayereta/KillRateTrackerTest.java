package com.slayereta;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class KillRateTrackerTest
{
	private static final long PAUSE_MS = 120_000;
	private static final long MIN = 60_000;

	@Test
	public void firstKillOnlyStartsTheClock()
	{
		KillRateTracker t = new KillRateTracker();
		t.recordKills(1, 0, PAUSE_MS);
		assertEquals(0, t.getCountedKills());
		assertEquals(0, t.getKillsPerHour(), 0.001);
	}

	@Test
	public void steadyKillEvery30sIs120PerHour()
	{
		KillRateTracker t = new KillRateTracker();
		for (int i = 0; i <= 10; i++)
		{
			t.recordKills(1, i * 30_000L, PAUSE_MS);
		}
		assertEquals(10, t.getCountedKills());
		assertEquals(120, t.getKillsPerHour(), 0.001);
		assertEquals(300_000, t.getKillingMs());
	}

	@Test
	public void gapEqualToThePauseTimeStillCounts()
	{
		KillRateTracker t = new KillRateTracker();
		t.recordKills(1, 0, PAUSE_MS);
		t.recordKills(1, PAUSE_MS, PAUSE_MS);
		assertEquals(1, t.getCountedKills());
		assertEquals(0, t.getBreakMs());
	}

	@Test
	public void pausesAreLeftOutOfTheRate()
	{
		KillRateTracker t = new KillRateTracker();
		t.recordKills(1, 0, PAUSE_MS);
		t.recordKills(1, 30_000, PAUSE_MS);
		t.recordKills(1, 60_000, PAUSE_MS);
		// 10 minute bank trip: neither the gap nor the kill that ended it counts
		t.recordKills(1, 660_000, PAUSE_MS);
		t.recordKills(1, 690_000, PAUSE_MS);
		assertEquals(3, t.getCountedKills());
		assertEquals(120, t.getKillsPerHour(), 0.001);
	}

	@Test
	public void aPauseUnderFifteenMinutesIsRememberedAsABreak()
	{
		KillRateTracker t = new KillRateTracker();
		t.recordKills(1, 0, PAUSE_MS);
		t.recordKills(1, 30_000, PAUSE_MS);
		t.recordKills(1, 30_000 + 5 * MIN, PAUSE_MS);
		assertEquals(5 * MIN, t.getBreakMs());
		assertEquals(30_000, t.getKillingMs());
	}

	@Test
	public void aPauseOverFifteenMinutesIsNotABreak()
	{
		KillRateTracker t = new KillRateTracker();
		t.recordKills(1, 0, PAUSE_MS);
		t.recordKills(1, 15 * MIN, PAUSE_MS);
		assertEquals(15 * MIN, t.getBreakMs());

		t.recordKills(1, 15 * MIN + 15 * MIN + 1, PAUSE_MS);
		assertEquals(15 * MIN, t.getBreakMs());
		assertEquals(0, t.getCountedKills());
	}

	@Test
	public void multiKillTickCountsEveryKill()
	{
		KillRateTracker t = new KillRateTracker();
		t.recordKills(1, 0, PAUSE_MS);
		t.recordKills(3, 60_000, PAUSE_MS);
		assertEquals(3, t.getCountedKills());
		assertEquals(180, t.getKillsPerHour(), 0.001);
	}

	@Test
	public void restartClockLeavesASuperiorFightOutOfBothRateAndBreaks()
	{
		KillRateTracker t = new KillRateTracker();
		t.recordKills(1, 0, PAUSE_MS);
		t.recordKills(1, 30_000, PAUSE_MS);
		// superior kill 5 minutes later: clock restarts, nothing counted, no break
		t.restartClock(30_000 + 5 * MIN);
		t.recordKills(1, 60_000 + 5 * MIN, PAUSE_MS);
		assertEquals(2, t.getCountedKills());
		assertEquals(120, t.getKillsPerHour(), 0.001);
		assertEquals(0, t.getBreakMs());
	}

	@Test
	public void clearClockMeansTimeLoggedOutIsNotABreak()
	{
		KillRateTracker t = new KillRateTracker();
		t.recordKills(1, 0, PAUSE_MS);
		t.recordKills(1, 30_000, PAUSE_MS);
		t.clearClock();
		// back 10 minutes later: this kill only starts the clock again
		t.recordKills(1, 30_000 + 10 * MIN, PAUSE_MS);
		assertEquals(0, t.getBreakMs());
		assertEquals(1, t.getCountedKills());
		t.recordKills(1, 60_000 + 10 * MIN, PAUSE_MS);
		assertEquals(2, t.getCountedKills());
		assertEquals(120, t.getKillsPerHour(), 0.001);
	}

	@Test
	public void rollingAverageFollowsRecentPace()
	{
		KillRateTracker t = new KillRateTracker();
		long now = 0;
		t.recordKills(1, now, PAUSE_MS);
		// 60 kills at 36s each (100/hr)
		for (int i = 0; i < 60; i++)
		{
			now += 36_000;
			t.recordKills(1, now, PAUSE_MS);
		}
		// 25 kills at 24s each (150/hr)
		for (int i = 0; i < 25; i++)
		{
			now += 24_000;
			t.recordKills(1, now, PAUSE_MS);
		}
		assertEquals(150, t.getKillsPerHour(25), 0.001);
		// whole-task average is still dragged down by the slower kills
		assertEquals(85 * 3_600_000d / (60 * 36_000 + 25 * 24_000), t.getKillsPerHour(), 0.001);
	}

	@Test
	public void rollingWithFewerKillsUsesWhatThereIs()
	{
		KillRateTracker t = new KillRateTracker();
		t.recordKills(1, 0, PAUSE_MS);
		t.recordKills(1, 30_000, PAUSE_MS);
		assertEquals(120, t.getKillsPerHour(25), 0.001);
	}

	@Test
	public void noBreakEstimateUntilTenMinutesOfKillingWhenNothingIsSaved()
	{
		KillRateTracker t = new KillRateTracker();
		long now = 0;
		t.recordKills(1, now, PAUSE_MS);
		// 5 minutes of killing, then a 3 minute break
		for (int i = 0; i < 10; i++)
		{
			now += 30_000;
			t.recordKills(1, now, PAUSE_MS);
		}
		now += 3 * MIN;
		t.recordKills(1, now, PAUSE_MS);
		assertEquals(3 * MIN, t.getBreakMs());
		assertEquals(-1, t.getBreakRatio(-1), 0.0001);

		// 5 more minutes of killing reaches the 10 minute mark: 3 min of breaks per 10 min of killing
		t.recordKills(1, now + 30_000, PAUSE_MS);
		now += 30_000;
		for (int i = 0; i < 9; i++)
		{
			now += 30_000;
			t.recordKills(1, now, PAUSE_MS);
		}
		assertEquals(10 * MIN, t.getKillingMs());
		assertEquals(0.3, t.getBreakRatio(-1), 0.0001);
	}

	@Test
	public void aSavedEstimateIsUsedFromTheFirstKill()
	{
		KillRateTracker t = new KillRateTracker();
		assertEquals(0.2, t.getBreakRatio(0.2), 0.0001);
		assertEquals(0.0, t.getBreakRatio(0.0), 0.0001);
	}

	@Test
	public void aSavedEstimateCountsAsThirtyMinutesOfKilling()
	{
		// saved: 0.2 (6 minutes of breaks per 30 of killing). This session: 5 minutes of breaks in 10 of killing.
		double blended = KillRateTracker.breakRatio(0.2, 10 * MIN, 5 * MIN);
		assertEquals((6.0 + 5.0) / 40.0, blended, 0.0001);
	}

	@Test
	public void aLongSessionGraduallyTakesOverFromTheSavedEstimate()
	{
		double early = KillRateTracker.breakRatio(0.5, 10 * MIN, 0);
		double late = KillRateTracker.breakRatio(0.5, 300 * MIN, 0);
		// both pulled towards the session's 0, the longer session far more
		assertEquals(0.5 * 30 / 40, early, 0.0001);
		assertEquals(0.5 * 30 / 330, late, 0.0001);
	}

	@Test
	public void resetForgetsEverything()
	{
		KillRateTracker t = new KillRateTracker();
		t.recordKills(1, 0, PAUSE_MS);
		t.recordKills(1, 30_000, PAUSE_MS);
		t.recordKills(1, 30_000 + 5 * MIN, PAUSE_MS);
		t.reset();
		assertEquals(0, t.getCountedKills());
		assertEquals(0, t.getKillingMs());
		assertEquals(0, t.getBreakMs());
		assertEquals(0, t.getKillsPerHour(), 0.001);
		// and the next kill only starts the clock again
		t.recordKills(1, 10 * MIN, PAUSE_MS);
		assertEquals(0, t.getCountedKills());
	}
}
