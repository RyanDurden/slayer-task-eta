# Slayer Task ETA

Shows how long is left on your current Slayer task, based on how fast you're killing.

## The overlay

By default the overlay shows:
- your task and how many kills are left
- your kills per hour
- the estimated time to finish (ETA)
- the time of day the task should finish ("Done at")

You can also switch on:
- **Time on task**: how long you've been on the task since your first kill. Time spent logged out isn't counted, so an idle logout or a disconnect won't wipe it. It starts again when you get a new task or your task is extended.

Every line can be switched off in the settings, and if they are all off nothing is drawn. **Mini mode** shrinks the overlay to just the ETA and kills left, or only one of them.

The ETA can be written as `1h 23m` or `1:23:00`, and the finish time as 24-hour or 12-hour.

## When it shows

The overlay appears when you get a task kill, or when you check your task with an enchanted (or eternal) gem, Slayer helmet or Slayer ring. It never appears on login.

It hides after 5 minutes without a task kill (change it in the settings, or set it to 0 to never hide). It never hides sooner than "Pause time after", so it can't vanish in the middle of a slow kill.

## How the kill rate works

The plugin reads your task and remaining kill count from RuneLite's built-in Slayer plugin, which must be enabled.

It times the gaps between task kills. If you go longer than **Pause time after** (2 minutes by default) without a task kill, that stretch is left out of the rate, along with the kill that ended it. That keeps banking, walking and AFK time from dragging your kills/hr down, so it reflects how fast you kill while you are actually killing.

**Slow kills, such as Slayer bosses:** if one kill takes longer than 2 minutes, raise "Pause time after" so those kills still count.

The first kill only starts the clock, so with the default of 3 kills before an ETA, your live rate appears on the 4th kill.

You can average over the whole task or over only your last N kills, which reacts faster if you change spot or gear.

Superior Slayer monsters are left out of the rate. When "A superior foe has appeared..." shows, the next task kill (within 10 minutes) is treated as the superior.

## Breaks in the ETA

Your kills/hr leaves breaks out, but the ETA and finish time can allow for them. A pause of up to 15 minutes is counted as a break. The ETA is then your killing time left, plus the break time you usually take for each hour of killing. Time spent logged out is never counted as a break. You can switch this off with **Include breaks in ETA**, and your kills/hr isn't affected either way.

With nothing saved yet, the ETA is killing time only until you have 10 minutes of killing behind you. After that your breaks are included.

## Saved rates

Your kills/hr and break time for each task are remembered (switch this off with **Remember rates per task**), so the next time you get that task the ETA shows straight away, in the saved-rate colour, until there are enough fresh kills to use a live rate. Your saved break time counts as about 30 minutes of past play and your current session is added to it, so one odd bank trip only nudges it and a long session gradually takes over.

## Right-click options

- **Reset session rate**: restarts this session's live rate and break time. Your saved values and time on task are kept.
- **Clear saved rate**: forgets the remembered rate and break time for the current task. Hidden by default; switch it on in the settings.

## Notifications

Optional (off by default). Get a notification when a set number of kills, or a set amount of time, is left on your task.
