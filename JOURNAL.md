# Journal

## 2026-08-19 — Initial implementation

Created the plugin from scratch: project scaffolding copied from `runelite_ge_helper`
(Gradle 8.5 wrapper, `latest.release` RuneLite client, Java 11 target, `installSideload`
task), package `com.github.ilee2.prayeralert`, root project name `prayer-alert`.

### Classes

- `CombatStyle` — MELEE / RANGED / MAGIC / UNKNOWN.
- `OverheadPrayer` — the 15 overhead icons, ordinal-aligned with the prayer sprite archive,
  each carrying the set of styles it blocks. Retribution, Smite, Redemption, Wrath and Soul
  Split block nothing.
- `OverheadPrayerReader` — turns `NPC#getOverheadArchiveIds()` / `getOverheadSpriteIds()`
  into a set of `OverheadPrayer`.
- `AttackStyleResolver` — cache-driven weapon/attack-style lookup.
- `AlertTone` / `AlertToneGenerator` — synthesises the alert tone as an in-memory WAV.
- `PrayerAlertPlugin` / `PrayerAlertConfig` / `PrayerAlertOverlay`.

### Decisions

**NPC overheads are raw sprite ids, not `HeadIcon`.** `HeadIcon` is exposed on `Player` only.
NPCs carry parallel archive-id / sprite-id arrays, which is exactly what allows several icons
at once — the thing that made "monsters sometimes pray multiple stuff" worth handling
properly rather than reading a single icon.

Archive **440** is the overhead prayer archive. Established from
`SpriteID.OVERHEAD_PROTECT_FROM_MELEE = 440` in `runelite-api` — the old flat `SpriteID` table
listed one constant per archive, named after the archive's first sprite, and 439/441 either
side are the PK skull archive and a minimap arrow. Sprite index then lines up with `HeadIcon`
ordinal order. Icons from any other archive are ignored rather than guessed at, so a boss with
custom overhead art produces silence, not noise.

**Combat style is read from the game cache, not a weapon table.** Mirrors the client's own
`AttackStylesPlugin`: enum 3908 maps weapon category to a style enum, whose structs carry the
style name in param 1407. The `WeaponType` enum RuneLite used to hardcode was deleted in
March 2024 for exactly this reason — new weapon categories used to need a client update.

Two corrections on top:

1. *Powered staves.* Trident/sanguinesti/shadow/sceptres train Magic, but their style names
   are `Accurate / Accurate / — / Longrange`, so names alone read as melee. Detected by the
   shape of the style list (nothing else repeats `Accurate` in the first two slots) rather
   than a category id, which would rot.
2. *Manual casts.* A hand-cast spell never touches the attack style varp, so a `Cast` menu
   click on an NPC marks the next 5 ticks as magic.

**Unknown resolves to silence.** Every path that cannot read state confidently returns
`CombatStyle.UNKNOWN`, which never alerts. A false beep every 1.8 seconds is worse than a
missed one, and the Debug readout option exists to identify anything misread.

**Alert gating.** The mismatch check runs on `GameTick` and needs: the player interacting with
a live NPC, a recent attack animation (configurable, on by default — stops the alert when you
click away mid-fight), the mismatch surviving a grace period (1 tick, so a prayer flick you
are already reacting to does not fire), and the style's alert toggle enabled. Sound repeats on
an interval; the chat line and tray notification fire once per mismatch.

**Synthesised beep over a bundled WAV.** `Client#playSoundEffect` follows the in-game sound
volume, and an alert silently muted along with the game is worse than useless. `AudioPlayer`
plays generated PCM at its own gain instead, with pitch/length/count configurable and no
binary asset in the repo. Game sound effects remain available as an option. Playback is
handed to the injected executor rather than blocking the client thread on opening a line.

### Multiple sideloaded plugins

`PluginManager#loadSideLoadPlugins` iterates every `.jar` in `~/.runelite/sideloaded-plugins`,
gives each its own `PluginClassLoader`, and catches failures per jar — so plugins are fully
independent and there is nothing to merge.

Generalised `runelite_ge_helper/scripts/Launch-RuneLiteDev.ps1` accordingly: instead of
syncing one hardcoded `ge-helper-*.jar`, it now walks sibling project folders under
`custom_plugins/`, reads `rootProject.name` from each `settings.gradle`, and refreshes
`<name>.jar` in the sideload directory from that project's newest build. Existing Steam
shortcut and all other behaviour unchanged.

### Tests

`OverheadPrayerTest` covers the sprite-index mapping, the combined and deflect icons, the
non-protective prayers, and unioning several overheads. `AlertToneGeneratorTest` checks that
every tone parses through `AudioSystem`, starts and ends at silence so it does not click, is
normalised below full scale, and that the synthesised tones actually decay rather than hold.

## 2026-08-19 — Alert tones

First in-game test worked, but the default sound was reported as annoying. Replaced the single
sine beep with four selectable tones (`AlertTone`), defaulting to **Chime**.

What actually made the old sound grating was timbre and envelope, not volume: a bare sine at
constant amplitude has no attack and no decay, so it reads as a smoke-alarm chirp. The three new
tones are additive synthesis — a fundamental plus a few partials under an exponential decay —
which is the envelope struck instruments have, and each uses two notes a consonant interval
apart so it reads as a signal rather than a fault.

- **Chime** — A5 → E6 (rising fifth), partials 1 / 2 / 3 / 4.2. The slightly inharmonic 4.2
  partial is what gives it bell shimmer. τ = 150 ms, 560 ms total.
- **Marimba** — E5 → B5, partials 1 / 4 / 10. Real marimba bars are tuned to their 4th partial,
  hence the ratio. τ = 95 ms, drier and shorter.
- **Soft blip** — single E5, faint 2nd harmonic. 260 ms.
- **Plain beep** — the previous behaviour, still driven by the pitch/length/count settings, which
  are now labelled "Plain beep only" since the other tones ignore them.

Output is normalised to a fixed 0.85 peak after synthesis, so stacking partials cannot clip and
switching tones does not change perceived loudness — that stays entirely on the Volume slider.

`SoundMode.BEEP` kept its constant name (display text changed to "Synthesised tone") so the saved
`prayeralert.soundMode=BEEP` in existing profiles still parses.

Two bugs found by the widened tests:

1. The end-of-buffer release ramp scaled the last sample by `1/release` rather than 0, leaving a
   step down to silence — exactly the click the ramp exists to prevent. Off-by-one in the ramp
   divisor.
2. A test expectation, not the code: the plain beep's inter-tone gap is `toneSamples / 2` in whole
   samples, so three 50 ms tones total 8819 samples, not a round 8820.
