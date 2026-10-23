package com.slayereta;

/** What the "task nearly done" notification waits for. */
public enum NotifyTrigger
{
	KILLS_LEFT("Kills left"),
	TIME_LEFT("Time left");

	private final String name;

	NotifyTrigger(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
