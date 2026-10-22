package com.slayereta;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class KillRateTrackerTest
{
	private static final long BREAK_MS = 120_000;

	@Test
	public void firstKillOnlyStartsTheClock()
	{
		KillRateTracker t = new KillRateTracker();
		t.recordKills(1, 0, BREAK_MS);
		assertEquals(0, t.getCountedKills());
		assertEquals(0, t.getKillsPerHour(), 0.001);
	}

	@Test
	public void steadyKillEvery30sIs120PerHour()
	{
		KillRateTracker t = new KillRateTracker();
		for (int i = 0; i <= 10; i++)
		{
			t.recordKills(1, i * 30_000L, BREAK_MS);
		}
		assertEquals(10, t.getCountedKills());
		assertEquals(120, t.getKillsPerHour(), 0.001);
	}

	@Test
	public void breaksAreLeftOutOfTheRate()
	{
		KillRateTracker t = new KillRateTracker();
		t.recordKills(1, 0, BREAK_MS);
		t.recordKills(1, 30_000, BREAK_MS);
		t.recordKills(1, 60_000, BREAK_MS);
		// 10 minute bank trip
		t.recordKills(1, 660_000, BREAK_MS);
		t.recordKills(1, 690_000, BREAK_MS);
		assertEquals(3, t.getCountedKills());
		assertEquals(120, t.getKillsPerHour(), 0.001);
	}

	@Test
	public void multiKillTickCountsEveryKill()
	{
		KillRateTracker t = new KillRateTracker();
		t.recordKills(1, 0, BREAK_MS);
		t.recordKills(3, 60_000, BREAK_MS);
		assertEquals(3, t.getCountedKills());
		assertEquals(180, t.getKillsPerHour(), 0.001);
	}

	@Test
	public void durationFormatting()
	{
		assertEquals("45s", SlayerEtaOverlay.formatDuration(45));
		assertEquals("12m 5s", SlayerEtaOverlay.formatDuration(725));
		assertEquals("1h 23m", SlayerEtaOverlay.formatDuration(4980));
	}
}
