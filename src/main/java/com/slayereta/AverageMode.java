package com.slayereta;

/** Which kills the kills/hr figure is averaged over. */
public enum AverageMode
{
	WHOLE_TASK("Whole task"),
	LAST_KILLS("Last N kills");

	private final String name;

	AverageMode(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
