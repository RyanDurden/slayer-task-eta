package com.slayereta;

/** How a length of time is written, for the ETA and time on task. */
public enum EtaFormat
{
	WORDS("1h 23m"),
	DIGITAL("1:23:00");

	private final String name;

	EtaFormat(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
