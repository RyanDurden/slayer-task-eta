package com.slayereta;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(SlayerEtaConfig.GROUP)
public interface SlayerEtaConfig extends Config
{
	String GROUP = "slayertasketa";

	// ------------------------------------------------------------------ sections

	@ConfigSection(
		name = "Display",
		description = "What the overlay shows, and when it hides",
		position = 0
	)
	String displaySection = "display";

	@ConfigSection(
		name = "Rate calculation",
		description = "How your kill rate and ETA are worked out",
		position = 1
	)
	String rateSection = "rate";

	@ConfigSection(
		name = "Saved rates",
		description = "Remembering your kill rate for each task",
		position = 2
	)
	String savedSection = "saved";

	@ConfigSection(
		name = "Right-click options",
		description = "Options shown when right-clicking the overlay",
		position = 3
	)
	String menuSection = "menu";

	@ConfigSection(
		name = "Notifications",
		description = "Alert when the task is nearly finished",
		position = 4,
		closedByDefault = true
	)
	String notifySection = "notifications";

	// ------------------------------------------------------------------ display

	@ConfigItem(
		keyName = "showTask",
		name = "Show task and kills left",
		description = "Show the task name and how many kills are left",
		position = 0,
		section = displaySection
	)
	default boolean showTask()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showKph",
		name = "Show kills/hr",
		description = "Show your current kill rate",
		position = 1,
		section = displaySection
	)
	default boolean showKph()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showEta",
		name = "Show ETA",
		description = "Show how long is left on the task",
		position = 2,
		section = displaySection
	)
	default boolean showEta()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFinishTime",
		name = "Show finish clock time",
		description = "Show the time of day the task should finish",
		position = 3,
		section = displaySection
	)
	default boolean showFinishTime()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showTimeOnTask",
		name = "Show time on task",
		description = "Show how long you've been on this task since your first kill. Time spent logged out isn't counted.",
		position = 4,
		section = displaySection
	)
	default boolean showTimeOnTask()
	{
		return false;
	}

	@ConfigItem(
		keyName = "etaFormat",
		name = "ETA format",
		description = "How the time left is written",
		position = 5,
		section = displaySection
	)
	default EtaFormat etaFormat()
	{
		return EtaFormat.WORDS;
	}

	@ConfigItem(
		keyName = "clockFormat",
		name = "Clock format",
		description = "How the finish time of day is written",
		position = 6,
		section = displaySection
	)
	default ClockFormat clockFormat()
	{
		return ClockFormat.TWENTY_FOUR_HOUR;
	}

	@ConfigItem(
		keyName = "liveColour",
		name = "Live rate colour",
		description = "Colour of the numbers when using your live kill rate",
		position = 7,
		section = displaySection
	)
	default Color liveColour()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "savedColour",
		name = "Saved rate colour",
		description = "Colour of the numbers while using a rate remembered from a previous session",
		position = 8,
		section = displaySection
	)
	default Color savedColour()
	{
		return new Color(200, 200, 120);
	}

	@ConfigItem(
		keyName = "hideAfterMinutes",
		name = "Hide after",
		description = "Hide the overlay this long after your last task kill or task check (0 = never hide). It never hides sooner than 'Pause time after'.",
		position = 9,
		section = displaySection
	)
	@Units(Units.MINUTES)
	@Range(min = 0, max = 120)
	default int hideAfterMinutes()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "miniMode",
		name = "Mini mode",
		description = "Show a smaller overlay with just the ETA and kills left. The 'Show' options above don't apply to it.",
		position = 10,
		section = displaySection
	)
	default boolean miniMode()
	{
		return false;
	}

	@ConfigItem(
		keyName = "miniContent",
		name = "Mini mode shows",
		description = "What the mini overlay shows",
		position = 11,
		section = displaySection
	)
	default MiniContent miniContent()
	{
		return MiniContent.BOTH;
	}

	// ------------------------------------------------------------------ rate calculation

	@ConfigItem(
		keyName = "idleThreshold",
		name = "Pause time after",
		description = "If you go longer than this without a task kill, that stretch is left out of your kill rate (banking, walking, AFK). Raise it for tasks where a single kill takes longer, such as Slayer bosses.",
		position = 0,
		section = rateSection
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
		position = 1,
		section = rateSection
	)
	@Range(min = 1, max = 50)
	default int minKills()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "includeBreaks",
		name = "Include breaks in ETA",
		description = "Add the time you usually spend on breaks (banking, walking) to the ETA and finish time. Your kills/hr isn't changed. Until it has seen enough of your breaks, the ETA is killing time only.",
		position = 2,
		section = rateSection
	)
	default boolean includeBreaks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "averageMode",
		name = "Average over",
		description = "Whole task: every kill counts equally. Last N kills: only your most recent kills count, so it reacts faster to changes in pace.",
		position = 3,
		section = rateSection
	)
	default AverageMode averageMode()
	{
		return AverageMode.WHOLE_TASK;
	}

	@ConfigItem(
		keyName = "rollingKills",
		name = "N (last N kills)",
		description = "How many recent kills to average over when using 'Last N kills'",
		position = 4,
		section = rateSection
	)
	@Range(min = 5, max = 200)
	default int rollingKills()
	{
		return 25;
	}

	// ------------------------------------------------------------------ saved rates

	@ConfigItem(
		keyName = "rememberRates",
		name = "Remember rates per task",
		description = "Save your kill rate and break time for each task so the ETA shows straight away next time you get it",
		position = 0,
		section = savedSection
	)
	default boolean rememberRates()
	{
		return true;
	}

	// ------------------------------------------------------------------ right-click

	@ConfigItem(
		keyName = "menuResetSession",
		name = "Show 'Reset session rate'",
		description = "Right-click option that restarts this session's live rate and break time. Your saved rate and time on task are not touched.",
		position = 0,
		section = menuSection
	)
	default boolean menuResetSession()
	{
		return true;
	}

	@ConfigItem(
		keyName = "menuClearSaved",
		name = "Show 'Clear saved rate'",
		description = "Right-click option that deletes the remembered rate and break time for the current task",
		position = 1,
		section = menuSection
	)
	default boolean menuClearSaved()
	{
		return false;
	}

	// ------------------------------------------------------------------ notifications

	@ConfigItem(
		keyName = "notification",
		name = "Task nearly done",
		description = "Send a notification when the task is nearly finished",
		position = 0,
		section = notifySection
	)
	default Notification notification()
	{
		return Notification.OFF;
	}

	@ConfigItem(
		keyName = "notifyTrigger",
		name = "Notify when",
		description = "Whether to notify based on kills left or time left",
		position = 1,
		section = notifySection
	)
	default NotifyTrigger notifyTrigger()
	{
		return NotifyTrigger.KILLS_LEFT;
	}

	@ConfigItem(
		keyName = "notifyKills",
		name = "Kills left",
		description = "Notify when this many kills are left",
		position = 2,
		section = notifySection
	)
	@Range(min = 1, max = 500)
	default int notifyKills()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "notifyMinutes",
		name = "Time left",
		description = "Notify when the ETA drops to this many minutes",
		position = 3,
		section = notifySection
	)
	@Units(Units.MINUTES)
	@Range(min = 1, max = 120)
	default int notifyMinutes()
	{
		return 2;
	}
}
