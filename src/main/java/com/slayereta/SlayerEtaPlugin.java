package com.slayereta;

import com.google.inject.Provides;
import java.util.Locale;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.slayer.SlayerPlugin;
import net.runelite.client.plugins.slayer.SlayerPluginService;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Slayer Task ETA",
	description = "Shows how long until your current Slayer task is finished, based on your kill rate",
	tags = {"slayer", "eta", "kills", "kph", "timer", "task"}
)
@PluginDependency(SlayerPlugin.class)
public class SlayerEtaPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(SlayerEtaPlugin.class);

	private static final String RATE_KEY_PREFIX = "kph_";
	private static final String BREAKS_KEY_PREFIX = "breaks_";
	private static final int SAVE_EVERY_KILLS = 10;

	/** After the superior message, the next task kill is treated as the superior for this long. */
	private static final long SUPERIOR_WINDOW_MS = 10 * 60_000L;

	private static final String SUPERIOR_MESSAGE = "A superior foe has appeared...";
	// Checking the task with an enchanted gem, Slayer helmet or Slayer ring. The
	// menu click below is the main way this is noticed; the chat message is a backup.
	private static final String TASK_CHECK_MESSAGE = "You're assigned to kill";
	private static final String CHECK_OPTION = "Check";

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

	@Inject
	private Notifier notifier;

	@Inject
	private ClientThread clientThread;

	@Inject
	private Client client;

	private final KillRateTracker tracker = new KillRateTracker();
	private final TimeOnTask timeOnTask = new TimeOnTask();

	private String currentTask;
	private int remaining;
	private int lastRemaining = -1;

	/** Kills/hr remembered from a previous session for the current task, or 0. */
	private double savedKph;

	/**
	 * Break estimate from before this session's kills (see {@link KillRateTracker#breakRatio}),
	 * or -1 for none. Set when a task starts and not changed by saving, so this
	 * session's numbers are never counted twice.
	 */
	private double priorBreakRatio = -1;

	private boolean visible;
	private long lastShowMs;

	private int killsAtLastSave;
	private boolean notified;

	/** Set on logout; the first tick after logging back in takes the task count as it finds it. */
	private boolean resync;

	/** When the superior message was seen, or -1. */
	private long superiorSinceMs = -1;

	@Provides
	SlayerEtaConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SlayerEtaConfig.class);
	}

	@Override
	protected void startUp()
	{
		clearTask();
		overlay.rebuildMenu();
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		saveRate();
		overlayManager.remove(overlay);
		clearTask();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!SlayerEtaConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		// Saving a rate also reports a change in this group; only real settings matter here.
		String key = event.getKey();
		if (key != null && (key.startsWith(RATE_KEY_PREFIX) || key.startsWith(BREAKS_KEY_PREFIX)))
		{
			return;
		}

		// Settings change on the UI thread; the overlay is drawn on the client thread.
		clientThread.invokeLater(overlay::rebuildMenu);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state != GameState.LOGIN_SCREEN && state != GameState.CONNECTION_LOST)
		{
			return;
		}

		// Logged out or dropped. Stop the time-on-task clock, and make sure the
		// time spent away is never counted as a pause or a break.
		timeOnTask.pause(clock());
		tracker.clearClock();
		superiorSinceMs = -1;
		resync = true;

		if (state == GameState.LOGIN_SCREEN)
		{
			// Never show on login; it comes back on a task kill or a task check.
			visible = false;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		long now = clock();
		// Ticks only happen while logged in, so this carries on after a logout.
		timeOnTask.resume(now);

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

		if (resync)
		{
			// First tick after a logout. The count may have changed while away (kills
			// made on mobile, say), and that isn't a kill we saw, so just take it as is.
			resync = false;
			lastRemaining = left;
			remaining = left;
			return;
		}

		if (left < lastRemaining)
		{
			int kills = lastRemaining - left;
			lastRemaining = left;
			remaining = left;
			onTaskKills(kills, now);
		}
		else if (left > lastRemaining)
		{
			// Task count went up (extended, or a fresh assignment of the same
			// monster). Keep the remembered rate but start timing again.
			saveRate();
			tracker.reset();
			timeOnTask.reset();
			killsAtLastSave = 0;
			notified = false;
			superiorSinceMs = -1;
			savedKph = loadRate(task);
			priorBreakRatio = loadBreakRatio(task);
		}

		lastRemaining = left;
		remaining = left;

		long hideDelayMs = EtaMath.hideDelayMs(config.hideAfterMinutes(), config.idleThreshold());
		if (visible && hideDelayMs > 0 && now - lastShowMs > hideDelayMs)
		{
			visible = false;
		}
	}

	private void onTaskKills(int kills, long now)
	{
		if (superiorPending(now))
		{
			// Leave the superior fight out of the rate, and don't count it as a break.
			tracker.restartClock(now);
			superiorSinceMs = -1;
		}
		else
		{
			tracker.recordKills(kills, now, config.idleThreshold() * 1000L);
		}

		timeOnTask.start(now);
		show(now);

		if (tracker.getCountedKills() - killsAtLastSave >= SAVE_EVERY_KILLS)
		{
			saveRate();
		}

		checkNotification();
	}

	private boolean superiorPending(long now)
	{
		return superiorSinceMs >= 0 && now - superiorSinceMs <= SUPERIOR_WINDOW_MS;
	}

	// ------------------------------------------------------------------ chat and task checks

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		String message = Text.removeTags(event.getMessage());
		if (message.contains(SUPERIOR_MESSAGE))
		{
			superiorSinceMs = clock();
		}
		else if (message.startsWith(TASK_CHECK_MESSAGE))
		{
			show(clock());
		}
	}

	/**
	 * Notice "Check" being clicked on an enchanted gem, Slayer helmet or Slayer
	 * ring, the same way RuneLite's own Slayer plugin does.
	 */
	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!CHECK_OPTION.equals(event.getMenuOption()))
		{
			return;
		}

		MenuAction action = event.getMenuAction();
		if (action != MenuAction.CC_OP && action != MenuAction.CC_OP_LOW_PRIORITY)
		{
			return;
		}

		Widget widget = client.getWidget(event.getParam1());
		if (widget != null && event.getParam0() != -1)
		{
			widget = widget.getChild(event.getParam0());
		}
		if (widget == null)
		{
			return;
		}

		int itemId = widget.getItemId();
		if (itemId == -1)
		{
			// Equipped items keep the item on a child of the slot instead.
			Widget[] children = widget.getDynamicChildren();
			if (children != null)
			{
				for (Widget child : children)
				{
					itemId = child.getItemId();
					if (itemId != -1)
					{
						break;
					}
				}
			}
		}

		// Every helmet colour and ring charge maps to the base helmet / ring here.
		itemId = ItemVariationMapping.map(itemId);
		if (itemId == ItemID.SLAYER_HELM || itemId == ItemID.SLAYER_RING_8
			|| itemId == ItemID.SLAYER_GEM || itemId == ItemID.SLAYER_ETERNAL_GEM)
		{
			show(clock());
		}
	}

	// ------------------------------------------------------------------ notification

	private void checkNotification()
	{
		if (notified || !config.notification().isEnabled() || currentTask == null)
		{
			return;
		}

		if (config.notifyTrigger() == NotifyTrigger.KILLS_LEFT)
		{
			if (remaining <= config.notifyKills())
			{
				notified = true;
				notifier.notify(config.notification(),
					"Slayer task: " + remaining + (remaining == 1 ? " kill left" : " kills left"));
			}
			return;
		}

		long secondsLeft = getEtaSeconds();
		if (secondsLeft >= 0 && secondsLeft <= config.notifyMinutes() * 60L)
		{
			notified = true;
			notifier.notify(config.notification(),
				"Slayer task: about " + EtaMath.formatEta(secondsLeft, EtaFormat.WORDS) + " left");
		}
	}

	// ------------------------------------------------------------------ values for the overlay

	String getCurrentTask()
	{
		return currentTask;
	}

	int getRemaining()
	{
		return remaining;
	}

	boolean isVisible()
	{
		return visible;
	}

	/**
	 * The rate the ETA should use: the live rate once there are enough kills
	 * this session, otherwise the remembered rate for this task.
	 */
	double getEffectiveKph()
	{
		if (hasLiveRate())
		{
			if (config.averageMode() == AverageMode.LAST_KILLS)
			{
				double rolling = tracker.getKillsPerHour(config.rollingKills());
				if (rolling > 0)
				{
					return rolling;
				}
			}
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

	/**
	 * Seconds until the task is finished, or -1 if there is no rate yet. Breaks
	 * are allowed for when that setting is on and there is an estimate to use.
	 */
	long getEtaSeconds()
	{
		double breakRatio = -1;
		if (config.includeBreaks())
		{
			breakRatio = tracker.getBreakRatio(config.rememberRates() ? priorBreakRatio : -1);
		}
		return EtaMath.etaSeconds(remaining, getEffectiveKph(), breakRatio);
	}

	/** Time since the first task kill, not counting time logged out, in ms, or -1. */
	long getTimeOnTaskMs()
	{
		return timeOnTask.getElapsedMs(clock());
	}

	// ------------------------------------------------------------------ right-click actions

	/** Restart this session's live rate and break estimate. Saved values and time on task are kept. */
	void resetSessionRate()
	{
		tracker.reset();
		killsAtLastSave = 0;
	}

	/** Forget the remembered rate and break estimate for the current task. */
	void clearSavedRate()
	{
		if (currentTask == null)
		{
			return;
		}
		configManager.unsetRSProfileConfiguration(SlayerEtaConfig.GROUP, rateKey(currentTask));
		configManager.unsetRSProfileConfiguration(SlayerEtaConfig.GROUP, breaksKey(currentTask));
		savedKph = 0;
		priorBreakRatio = -1;
		killsAtLastSave = tracker.getCountedKills();
	}

	// ------------------------------------------------------------------ task state

	/** The current time in ms. Its own method so a test can drive time by hand. */
	long clock()
	{
		return System.currentTimeMillis();
	}

	private void show(long now)
	{
		if (currentTask == null)
		{
			// Nothing to show yet (no task), and it mustn't pop up later when one is given.
			return;
		}
		visible = true;
		lastShowMs = now;
	}

	private void startTask(String task, int left)
	{
		currentTask = task;
		remaining = left;
		lastRemaining = left;
		tracker.reset();
		timeOnTask.reset();
		killsAtLastSave = 0;
		notified = false;
		resync = false;
		superiorSinceMs = -1;
		savedKph = loadRate(task);
		priorBreakRatio = loadBreakRatio(task);
		log.debug("Tracking task {} ({} left), saved rate {}, saved break ratio {}",
			task, left, savedKph, priorBreakRatio);
	}

	private void clearTask()
	{
		currentTask = null;
		remaining = 0;
		lastRemaining = -1;
		savedKph = 0;
		priorBreakRatio = -1;
		killsAtLastSave = 0;
		notified = false;
		resync = false;
		superiorSinceMs = -1;
		visible = false;
		tracker.reset();
		timeOnTask.reset();
	}

	private void saveRate()
	{
		if (!config.rememberRates() || currentTask == null || !hasLiveRate())
		{
			return;
		}

		// Always save the whole-task rate: it's the steadier number to start from next time.
		double kph = tracker.getKillsPerHour();
		configManager.setRSProfileConfiguration(SlayerEtaConfig.GROUP, rateKey(currentTask),
			String.format(Locale.ROOT, "%.1f", kph));
		savedKph = kph;

		double breakRatio = tracker.getBreakRatio(priorBreakRatio);
		if (breakRatio >= 0)
		{
			configManager.setRSProfileConfiguration(SlayerEtaConfig.GROUP, breaksKey(currentTask),
				String.format(Locale.ROOT, "%.3f", breakRatio));
		}
		killsAtLastSave = tracker.getCountedKills();
	}

	private double loadRate(String task)
	{
		double value = loadNumber(rateKey(task));
		return value > 0 ? value : 0;
	}

	private double loadBreakRatio(String task)
	{
		double value = loadNumber(breaksKey(task));
		return value >= 0 ? value : -1;
	}

	/** A saved number, or -1 if there isn't one (or it can't be read). */
	private double loadNumber(String key)
	{
		String value = configManager.getRSProfileConfiguration(SlayerEtaConfig.GROUP, key);
		if (value == null)
		{
			return -1;
		}
		try
		{
			double number = Double.parseDouble(value);
			return Double.isFinite(number) ? number : -1;
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}

	private static String rateKey(String task)
	{
		return RATE_KEY_PREFIX + slug(task);
	}

	private static String breaksKey(String task)
	{
		return BREAKS_KEY_PREFIX + slug(task);
	}

	private static String slug(String task)
	{
		return task.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
	}
}
