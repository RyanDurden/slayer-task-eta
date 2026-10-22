# Slayer Task ETA

Shows how long is left on your current Slayer task.

The overlay shows:
- your task and how many kills are left
- your kills per hour
- the estimated time to finish the task
- the time of day the task should finish

## How it works

The plugin reads your current task and remaining kill count from RuneLite's built-in Slayer plugin, which must be enabled.

It times the gaps between task kills. Gaps longer than the break threshold (2 minutes by default) are treated as breaks, such as banking or travelling, and are left out, so the rate reflects how fast you kill while on task.

The ETA appears after a few kills. Your rate for each task is remembered, so the next time you get that task the ETA shows straight away (in yellow) until there are enough fresh kills to use a live rate (in white).

## Settings

- **Break threshold**: gap between kills treated as a break
- **Kills before ETA**: kills needed this session before a live rate is used
- **Remember rates per task**: save your rate per task for next time
- **Show finish clock time**: show the time of day the task will finish
- **Show kills/hr**: show your current kill rate
