package com.slayereta;

import com.google.inject.Provides;
import java.util.Locale;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.slayer.SlayerPlugin;
import net.runelite.client.plugins.slayer.SlayerPluginService;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Slayer Task ETA",
	description = "Shows how long until your current Slayer task is finished, based on your kill rate",
	tags = {"slayer", "eta", "kills", "kph", "timer", "task"}
)
@PluginDependency(SlayerPlugin.class)
public class SlayerEtaPlugin extends Plugin
{
	private static final String RATE_KEY_PREFIX = "kph_";
	private static final int SAVE_EVERY_KILLS = 10;

	@Inject
	private SlayerPluginService slayerService;

	@Inject
	private SlayerEtaConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private SlayerEtaOverlay overlay;

	private final KillRateTracker tracker = new KillRateTracker();

	@Getter
	private String currentTask;

	@Getter
	private int remaining;

	/** Rate loaded from a previous session for the current task, or 0. */
	@Getter
	private double savedKph;

	private int lastRemaining = -1;
	private int killsAtLastSave;

	@Provides
	SlayerEtaConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SlayerEtaConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		clearTask();
	}

	@Override
	protected void shutDown()
	{
		saveRate();
		overlayManager.remove(overlay);
		clearTask();
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		String task = slayerService.getTask();
		int left = slayerService.getRemainingAmount();

		if (task == null || task.isEmpty() || left <= 0)
		{
			if (currentTask != null)
			{
				saveRate();
				clearTask();
			}
			return;
		}

		if (!task.equalsIgnoreCase(currentTask))
		{
			saveRate();
			startTask(task, left);
			return;
		}

		if (left < lastRemaining)
		{
			tracker.recordKills(lastRemaining - left, System.currentTimeMillis(),
				config.idleThreshold() * 1000L);

			if (tracker.getCountedKills() - killsAtLastSave >= SAVE_EVERY_KILLS)
			{
				saveRate();
			}
		}
		else if (left > lastRemaining)
		{
			// Task count went up (extended, or a fresh assignment of the same
			// monster). Keep the remembered rate but start timing again.
			saveRate();
			tracker.reset();
			killsAtLastSave = 0;
		}

		lastRemaining = left;
		remaining = left;
	}

	/**
	 * The rate the ETA should use: the live rate once there are enough kills
	 * this session, otherwise the remembered rate for this task.
	 */
	double getEffectiveKph()
	{
		if (hasLiveRate())
		{
			return tracker.getKillsPerHour();
		}
		return config.rememberRates() ? savedKph : 0;
	}

	boolean hasLiveRate()
	{
		return tracker.getCountedKills() >= config.minKills() && tracker.getKillsPerHour() > 0;
	}

	int getKillsUntilLiveRate()
	{
		return Math.max(0, config.minKills() - tracker.getCountedKills());
	}

	private void startTask(String task, int left)
	{
		currentTask = task;
		remaining = left;
		lastRemaining = left;
		tracker.reset();
		killsAtLastSave = 0;
		savedKph = loadRate(task);
		log.debug("Tracking task {} ({} left), saved rate {}", task, left, savedKph);
	}

	private void clearTask()
	{
		currentTask = null;
		remaining = 0;
		lastRemaining = -1;
		savedKph = 0;
		killsAtLastSave = 0;
		tracker.reset();
	}

	private void saveRate()
	{
		if (!config.rememberRates() || currentTask == null || !hasLiveRate())
		{
			return;
		}

		double kph = tracker.getKillsPerHour();
		configManager.setRSProfileConfiguration(SlayerEtaConfig.GROUP, rateKey(currentTask),
			String.format(Locale.ROOT, "%.1f", kph));
		savedKph = kph;
		killsAtLastSave = tracker.getCountedKills();
	}

	private double loadRate(String task)
	{
		String value = configManager.getRSProfileConfiguration(SlayerEtaConfig.GROUP, rateKey(task));
		if (value == null)
		{
			return 0;
		}
		try
		{
			return Double.parseDouble(value);
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	private static String rateKey(String task)
	{
		return RATE_KEY_PREFIX + task.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
	}
}
