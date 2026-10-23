package com.slayereta;

/** What the mini overlay shows. */
public enum MiniContent
{
	BOTH("ETA and kills left"),
	ETA_ONLY("ETA only"),
	KILLS_ONLY("Kills left only");

	private final String name;

	MiniContent(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
