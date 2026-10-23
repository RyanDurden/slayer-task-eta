package com.slayereta;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class SlayerEtaOverlay extends OverlayPanel
{
	private static final String MENU_TARGET = "Slayer ETA";
	private static final String RESET_SESSION = "Reset session rate";
	private static final String CLEAR_SAVED = "Clear saved rate";

	private final SlayerEtaPlugin plugin;
	private final SlayerEtaConfig config;

	@Inject
	SlayerEtaOverlay(SlayerEtaPlugin plugin, SlayerEtaConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	/** Add or remove the right-click options to match the settings. */
	void rebuildMenu()
	{
		removeMenuEntry(MenuAction.RUNELITE_OVERLAY, RESET_SESSION, MENU_TARGET);
		removeMenuEntry(MenuAction.RUNELITE_OVERLAY, CLEAR_SAVED, MENU_TARGET);
		if (config.menuResetSession())
		{
			addMenuEntry(MenuAction.RUNELITE_OVERLAY, RESET_SESSION, MENU_TARGET, e -> plugin.resetSessionRate());
		}
		if (config.menuClearSaved())
		{
			addMenuEntry(MenuAction.RUNELITE_OVERLAY, CLEAR_SAVED, MENU_TARGET, e -> plugin.clearSavedRate());
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		String task = plugin.getCurrentTask();
		if (task == null || !plugin.isVisible())
		{
			return null;
		}

		int left = plugin.getRemaining();
		double kph = plugin.getEffectiveKph();
		boolean live = plugin.hasLiveRate();
		long secondsLeft = plugin.getEtaSeconds();
		// Numbers worked out from the kill rate use the live or saved colour.
		Color rateColour = live ? config.liveColour() : config.savedColour();
		Color etaColour = secondsLeft >= 0 ? rateColour : Color.WHITE;

		if (config.miniMode())
		{
			// Mini mode ignores the per-line options; it only has its own choice.
			MiniContent content = config.miniContent();
			if (content != MiniContent.KILLS_ONLY)
			{
				panelComponent.getChildren().add(line("ETA", etaText(secondsLeft), etaColour));
			}
			if (content != MiniContent.ETA_ONLY)
			{
				panelComponent.getChildren().add(line("Left", String.valueOf(left), Color.WHITE));
			}
			return super.render(graphics);
		}

		List<LineComponent> lines = new ArrayList<>();

		if (config.showTask())
		{
			lines.add(line(task, left + " left", Color.WHITE));
		}

		if (config.showKph() && kph > 0)
		{
			lines.add(line(live ? "Kills/hr" : "Kills/hr (saved)", String.valueOf(Math.round(kph)), rateColour));
		}

		if (config.showEta())
		{
			lines.add(line("ETA", etaText(secondsLeft), etaColour));
		}

		if (config.showFinishTime() && secondsLeft >= 0)
		{
			lines.add(line("Done at", LocalTime.now().plusSeconds(secondsLeft)
				.format(config.clockFormat().getFormatter()), rateColour));
		}

		if (config.showTimeOnTask())
		{
			long onTaskMs = plugin.getTimeOnTaskMs();
			lines.add(line("Time on task",
				onTaskMs >= 0 ? EtaMath.formatEta(onTaskMs / 1000, config.etaFormat()) : "-", Color.WHITE));
		}

		if (lines.isEmpty())
		{
			// Every line is switched off (or has nothing to show): draw nothing at all.
			return null;
		}

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Slayer ETA")
			.build());
		panelComponent.getChildren().addAll(lines);
		return super.render(graphics);
	}

	private static LineComponent line(String left, String right, Color rightColour)
	{
		return LineComponent.builder()
			.left(left)
			.right(right)
			.rightColor(rightColour)
			.build();
	}

	private String etaText(long secondsLeft)
	{
		if (secondsLeft >= 0)
		{
			return EtaMath.formatEta(secondsLeft, config.etaFormat());
		}
		int more = plugin.getKillsUntilLiveRate();
		if (more <= 0)
		{
			return "-";
		}
		return more == 1 ? "1 more kill" : more + " more kills";
	}
}
