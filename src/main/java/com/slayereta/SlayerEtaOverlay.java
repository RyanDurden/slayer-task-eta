package com.slayereta;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class SlayerEtaOverlay extends OverlayPanel
{
	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
	private static final Color SAVED_RATE_COLOUR = new Color(200, 200, 120);

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

	@Override
	public Dimension render(Graphics2D graphics)
	{
		String task = plugin.getCurrentTask();
		if (task == null)
		{
			return null;
		}

		int left = plugin.getRemaining();
		double kph = plugin.getEffectiveKph();
		boolean live = plugin.hasLiveRate();

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Slayer ETA")
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left(task)
			.right(left + " left")
			.build());

		if (kph <= 0)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("ETA")
				.right(plugin.getKillsUntilLiveRate() + " more kills")
				.build());
			return super.render(graphics);
		}

		// Colour the numbers differently while still running on a rate
		// remembered from a previous session.
		Color colour = live ? Color.WHITE : SAVED_RATE_COLOUR;

		if (config.showKph())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(live ? "Kills/hr" : "Kills/hr (saved)")
				.right(String.valueOf(Math.round(kph)))
				.rightColor(colour)
				.build());
		}

		long secondsLeft = Math.round(left * 3600d / kph);
		panelComponent.getChildren().add(LineComponent.builder()
			.left("ETA")
			.right(formatDuration(secondsLeft))
			.rightColor(colour)
			.build());

		if (config.showFinishTime())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Done at")
				.right(LocalTime.now().plusSeconds(secondsLeft).format(CLOCK))
				.rightColor(colour)
				.build());
		}

		return super.render(graphics);
	}

	static String formatDuration(long totalSeconds)
	{
		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;
		if (hours > 0)
		{
			return hours + "h " + minutes + "m";
		}
		if (minutes > 0)
		{
			return minutes + "m " + seconds + "s";
		}
		return seconds + "s";
	}
}
