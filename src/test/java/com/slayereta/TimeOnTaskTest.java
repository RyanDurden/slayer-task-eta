package com.slayereta;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class TimeOnTaskTest
{
	@Test
	public void notStartedUntilTheFirstKill()
	{
		TimeOnTask t = new TimeOnTask();
		assertEquals(-1, t.getElapsedMs(1_000));
		t.resume(1_000);
		assertEquals(-1, t.getElapsedMs(2_000));
	}

	@Test
	public void countsFromTheFirstKill()
	{
		TimeOnTask t = new TimeOnTask();
		t.start(10_000);
		assertEquals(0, t.getElapsedMs(10_000));
		assertEquals(25_000, t.getElapsedMs(35_000));
	}

	@Test
	public void startingAgainDoesNotRestartIt()
	{
		TimeOnTask t = new TimeOnTask();
		t.start(10_000);
		t.start(50_000);
		assertEquals(60_000, t.getElapsedMs(70_000));
	}

	@Test
	public void loggedOutTimeIsNotCounted()
	{
		TimeOnTask t = new TimeOnTask();
		t.start(0);
		// 20 minutes on task, logged out for 10, back for 5
		t.pause(20 * 60_000L);
		assertEquals(20 * 60_000L, t.getElapsedMs(25 * 60_000L));
		t.resume(30 * 60_000L);
		assertEquals(25 * 60_000L, t.getElapsedMs(35 * 60_000L));
	}

	@Test
	public void pausingTwiceOrResumingTwiceChangesNothing()
	{
		TimeOnTask t = new TimeOnTask();
		t.start(0);
		t.pause(60_000);
		t.pause(120_000);
		assertEquals(60_000, t.getElapsedMs(200_000));
		t.resume(300_000);
		t.resume(400_000);
		assertEquals(60_000 + 100_000, t.getElapsedMs(400_000));
	}

	@Test
	public void resetStartsOver()
	{
		TimeOnTask t = new TimeOnTask();
		t.start(0);
		t.pause(60_000);
		t.reset();
		assertEquals(-1, t.getElapsedMs(100_000));
		t.start(100_000);
		assertEquals(10_000, t.getElapsedMs(110_000));
	}

	@Test
	public void aClockGoingBackwardsNeverGivesNegativeTime()
	{
		TimeOnTask t = new TimeOnTask();
		t.start(100_000);
		assertEquals(0, t.getElapsedMs(90_000));
		t.pause(90_000);
		assertEquals(0, t.getElapsedMs(90_000));
	}
}
