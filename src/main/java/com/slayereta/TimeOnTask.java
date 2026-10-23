package com.slayereta;

/**
 * Real time spent on the current task, counting only time spent logged in.
 *
 * It starts at the first task kill. Logging out pauses it and logging back in
 * carries on, so an idle logout or a disconnect doesn't wipe it.
 */
class TimeOnTask
{
	private boolean started;
	private long accumulatedMs;
	/** When the clock last started running, or -1 while paused. */
	private long runningSinceMs = -1;

	/** Start timing at {@code nowMs}. Does nothing if already started. */
	void start(long nowMs)
	{
		if (started)
		{
			return;
		}
		started = true;
		accumulatedMs = 0;
		runningSinceMs = nowMs;
	}

	/** Stop the clock, keeping what has been counted. Does nothing if not running. */
	void pause(long nowMs)
	{
		if (runningSinceMs < 0)
		{
			return;
		}
		accumulatedMs += Math.max(0, nowMs - runningSinceMs);
		runningSinceMs = -1;
	}

	/** Carry on after a pause. Does nothing if never started or already running. */
	void resume(long nowMs)
	{
		if (started && runningSinceMs < 0)
		{
			runningSinceMs = nowMs;
		}
	}

	/** Time on task so far in ms, or -1 if it hasn't started. */
	long getElapsedMs(long nowMs)
	{
		if (!started)
		{
			return -1;
		}
		long running = runningSinceMs >= 0 ? Math.max(0, nowMs - runningSinceMs) : 0;
		return accumulatedMs + running;
	}

	void reset()
	{
		started = false;
		accumulatedMs = 0;
		runningSinceMs = -1;
	}
}
