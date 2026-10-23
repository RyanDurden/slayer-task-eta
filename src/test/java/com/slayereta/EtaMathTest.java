package com.slayereta;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class EtaMathTest
{
	@Test
	public void noRateMeansNoEta()
	{
		assertEquals(-1, EtaMath.etaSeconds(100, 0, -1));
		assertEquals(-1, EtaMath.etaSeconds(100, -5, 0.5));
	}

	@Test
	public void etaWithoutBreaks()
	{
		// 60 kills at 120/hr is half an hour
		assertEquals(1800, EtaMath.etaSeconds(60, 120, -1));
		assertEquals(1800, EtaMath.etaSeconds(60, 120, 0));
	}

	@Test
	public void breaksStretchTheEta()
	{
		// 60 minutes of killing left with 5 minutes of breaks per 45 of killing
		assertEquals(Math.round(3600 * (1 + 5.0 / 45.0)), EtaMath.etaSeconds(120, 120, 5.0 / 45.0));
		// 0.25 means 15 minutes of breaks per hour of killing
		assertEquals(4500, EtaMath.etaSeconds(120, 120, 0.25));
	}

	@Test
	public void hideDelayIsTheLongerOfHideAfterAndPauseTime()
	{
		assertEquals(300_000, EtaMath.hideDelayMs(5, 120));
		assertEquals(120_000, EtaMath.hideDelayMs(1, 120));
		assertEquals(600_000, EtaMath.hideDelayMs(5, 600));
	}

	@Test
	public void hideAfterZeroMeansNever()
	{
		assertEquals(0, EtaMath.hideDelayMs(0, 120));
		assertEquals(0, EtaMath.hideDelayMs(0, 1800));
	}

	@Test
	public void etaFormatting()
	{
		assertEquals("45s", EtaMath.formatEta(45, EtaFormat.WORDS));
		assertEquals("12m 5s", EtaMath.formatEta(725, EtaFormat.WORDS));
		assertEquals("1h 23m", EtaMath.formatEta(4980, EtaFormat.WORDS));
		assertEquals("1:23:00", EtaMath.formatEta(4980, EtaFormat.DIGITAL));
		assertEquals("0:00:45", EtaMath.formatEta(45, EtaFormat.DIGITAL));
		assertEquals("0s", EtaMath.formatEta(0, EtaFormat.WORDS));
	}

	@Test
	public void clockFormatting()
	{
		java.time.LocalTime t = java.time.LocalTime.of(15, 7);
		assertEquals("15:07", t.format(ClockFormat.TWENTY_FOUR_HOUR.getFormatter()));
		assertEquals("3:07 PM", t.format(ClockFormat.TWELVE_HOUR.getFormatter()));
		assertEquals("12:00 AM", java.time.LocalTime.MIDNIGHT.format(ClockFormat.TWELVE_HOUR.getFormatter()));
	}
}
