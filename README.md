# Prayer Alert

A RuneLite plugin that beeps while you are attacking a monster with a combat style its
overhead prayer blocks, and goes quiet the moment you switch to something that gets through.

Hitting a Protect from Melee gorilla with a scimitar, a Protect from Missiles Phantom Muspah
with a bow, a Protect from Magic anything with a trident — all of it is a stream of zeroes
that is easy to miss when you are watching something else. This is an audible nudge, not a
combat aid: everything it reacts to is drawn above the monster's head already.

## What it does

- Watches the NPC you are attacking and every overhead prayer icon above it — monsters that
  show two or three icons at once are handled, as are the deflect icons.
- Works out what your current weapon and attack style actually deal damage with.
- If the two collide, it beeps on an interval until you fix it, change target, or stop
  attacking.

It never sends input or automates anything. It reads, it beeps, it draws.

## Options

**Sound**

| Option | Default | Notes |
| --- | --- | --- |
| Sound | Synthesised tone | Generated locally, independent of in-game volume. Also available: a game sound effect, or visual only. |
| Tone | Chime | `Chime`, `Marimba`, `Soft blip`, or `Plain beep`. See below. |
| Volume | 60% | 0 mutes the sound and leaves the on-screen warning working. |
| Beep pitch / length / count | 880 Hz, 70 ms, 2 | **Plain beep only** — ignored by the other tones. |
| Game sound id | 3924 | Used when Sound is set to Game sound effect. |
| Repeat every | 3 ticks | 1.8 s between beeps while the mismatch lasts. |
| Notification | Off | Optional tray notification on the first beep only. |

### Tones

What makes a repeating alert grating is timbre and envelope, not volume. A bare sine held at
constant amplitude has no attack and no decay, so it reads as a smoke-alarm chirp. The first
three tones are additive synthesis — a fundamental plus a few partials under an exponential
decay — which is what gives struck instruments their carrying-but-not-harsh character.

| Tone | Character |
| --- | --- |
| **Chime** (default) | Two notes rising a fifth (A5 → E6) with bell partials. Soft attack, long ring. |
| **Marimba** | Warm wooden double tap (E5 → B5), tuned to the 4th partial like a real bar. Shorter, drier. |
| **Soft blip** | One short round note. The most understated option. |
| **Plain beep** | The original bare sine, driven by the pitch/length/count settings. |

All tones are normalised to the same peak, so switching between them does not change how loud
the alert is — use **Volume** for that.

If it still nags, **Repeat every** is the other dial worth turning: at the default 3 ticks it
fires every 1.8 s, and 5–6 ticks is noticeably more relaxed while still being hard to miss.

**When to alert**

| Option | Default | Notes |
| --- | --- | --- |
| Protect from Melee / Missiles / Magic | all on | Turn off the ones you do not care about. |
| Only while swinging | on | Requires a recent attack animation, so standing next to a monster after clicking away stays quiet. |
| Swing timeout | 8 ticks | How long after your last swing the alert keeps running. |
| Grace period | 1 tick | How long a mismatch must last before the first beep — covers a prayer switch you are already reacting to. |
| Ignored NPCs | empty | Comma-separated names, `*` matches any run of characters. |

**Display**

| Option | Default | Notes |
| --- | --- | --- |
| Show warning over target | on | Draws e.g. `Melee blocked` above the monster. |
| Tint target | on | Shades the monster while the alert is active. |
| Warning colour | red | |
| Chat message | off | One line when a mismatch starts. |
| Debug readout | off | Shows detected weapon category, combat style and overheads. |

## Install

```
.\gradlew.bat installSideload
```

RuneLite only scans `~/.runelite/sideloaded-plugins` when the client is started with
`--developer-mode`, which the stock launcher refuses to pass through. `Launch-RuneLiteDev.ps1`,
kept next to the plugin checkouts rather than inside any one of them, starts the installed
client through the launcher's `--classpath` entry point, which is the one path that leaves
developer mode on. It also refreshes the sideloaded jar of every plugin project beside it on
each launch, so after the first install `.\gradlew.bat jar` plus a relaunch is enough.

## Development

```
.\gradlew.bat test     # unit tests
.\gradlew.bat jar      # build the sideload jar
.\gradlew.bat run      # launch RuneLite from source with the plugin loaded
```

See [CLAUDE.md](CLAUDE.md) for how the overhead icons and combat style are read out of the
client.
