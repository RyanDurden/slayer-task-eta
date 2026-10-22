package com.slayereta;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(SlayerEtaConfig.GROUP)
public interface SlayerEtaConfig extends Config
{
	String GROUP = "slayertasketa";

	@ConfigItem(
		keyName = "idleThreshold",
		name = "Break threshold",
		description = "A gap between kills longer than this is treated as a break (banking, AFK, travel) and left out of the kill rate",
		position = 1
	)
	@Units(Units.SECONDS)
	@Range(min = 10, max = 1800)
	default int idleThreshold()
	{
		return 120;
	}

	@ConfigItem(
		keyName = "minKills",
		name = "Kills before ETA",
		description = "How many kills to see this session before showing a live rate (a remembered rate from earlier sessions is shown until then)",
		position = 2
	)
	@Range(min = 1, max = 50)
	default int minKills()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "rememberRates",
		name = "Remember rates per task",
		description = "Save your kill rate for each task so the ETA shows straight away next time you get it",
		position = 3
	)
	default boolean rememberRates()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFinishTime",
		name = "Show finish clock time",
		description = "Also show the time of day the task should finish",
		position = 4
	)
	default boolean showFinishTime()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showKph",
		name = "Show kills/hr",
		description = "Show your current kill rate",
		position = 5
	)
	default boolean showKph()
	{
		return true;
	}
}
