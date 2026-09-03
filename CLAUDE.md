# RuneLite Prayer Alert Plugin

## Project Goal

Sound a repeating alert while the player attacks an NPC with a combat style that NPC's
overhead prayer protects against — melee into Protect from Melee, ranged into Protect from
Missiles, magic into Protect from Magic — and stop the moment the player switches to a style
that gets through. An NPC can display several overhead icons at once; every one of them is
checked.

This is a notification of information the game already draws on screen. It reads client
state, plays a sound, and draws an overlay. It never sends input, automates an action, or
surfaces anything a player could not see by looking at the monster.

## Architecture

| Class | Responsibility |
| --- | --- |
| `PrayerAlertPlugin` | Event wiring, per-tick mismatch check, alert throttling |
| `PrayerAlertConfig` | Config panel |
| `AttackStyleResolver` | Weapon + attack style → `CombatStyle` |
| `OverheadPrayerReader` | NPC overhead sprites → `OverheadPrayer` set |
| `OverheadPrayer` | Overhead icon → combat styles it blocks |
| `CombatStyle` | MELEE / RANGED / MAGIC / UNKNOWN |
| `AlertTone` | Tone shapes and their synthesis parameters |
| `AlertToneGenerator` | Renders an `AlertTone` to an in-memory WAV |
| `PrayerAlertOverlay` | Target highlight, warning text, debug readout |

### Reading the target's overhead prayers

`net.runelite.api.HeadIcon` only exists for players. NPCs expose overheads as parallel arrays
— `NPC#getOverheadArchiveIds()` and `NPC#getOverheadSpriteIds()` — which is what lets one NPC
show several icons at once.

- Sprite archive **440** holds the overhead prayer icons. It is the archive
  `SpriteID.OVERHEAD_PROTECT_FROM_MELEE` names; the old flat `SpriteID` table had one constant
  per archive, named after that archive's first sprite. An unset archive id (`-1`) means the
  same archive.
- Sprite index inside that archive matches `HeadIcon` ordinal order: melee, missiles, magic,
  retribution, smite, redemption, the three two-way combinations, protect-all, wrath,
  soul split, then the three deflect icons.
- Icons from any other archive, or indexes past the end, are ignored rather than guessed at.

### Reading the player's combat style

Mirrors the client's own Attack Styles plugin, reading the game cache instead of a hardcoded
weapon table so new weapon categories keep working without a plugin update:

- `VarbitID.COMBAT_WEAPON_CATEGORY` (357) → weapon category
- `VarPlayerID.COM_MODE` (43) → selected attack style index
- `VarbitID.AUTOCAST_DEFMODE` (2668) → added to index 4, which is how defensive casting is
  selected
- enum **3908** maps weapon category → an enum of attack-style structs
- struct param **1407** is the style name: `Accurate`, `Aggressive`, `Defensive`,
  `Controlled`, `Ranging`, `Longrange`, `Casting`, `Other`
- Categories 22 and 30 are absent from enum 3908; the client hardcodes them, and so does this
  plugin

Two corrections sit on top of the cache lookup:

1. **Powered staves** (trident, sanguinesti, shadow, sceptres) train Magic but their styles
   are named `Accurate / Accurate / — / Longrange`, which would otherwise read as melee. No
   other weapon repeats `Accurate` in the first two slots, so the shape of the style list
   identifies them without pinning the plugin to a category id.
2. **Manual spell casts** do not touch the attack style varp, so a `Cast` menu click on an NPC
   marks the next few ticks as magic.

Anything unrecognised resolves to `CombatStyle.UNKNOWN`, which never alerts. Silence beats a
wrong beep. The **Debug readout** config option prints the detected weapon category, style and
overheads so a misread weapon can be identified.

## Building & Running In-Game

**Any change to plugin source or resources requires a rebuild before the running client will
show it.** The client loads a packaged jar, not the working tree.

```
.\gradlew.bat jar
```

Then relaunch RuneLite. `runelite_ge_helper/scripts/Launch-RuneLiteDev.ps1` (the target of the
custom Steam shortcut) scans every sibling project folder under `custom_plugins/` and copies
each project's newest `build/libs/<rootProject.name>-*.jar` into
`~/.runelite/sideloaded-plugins/<rootProject.name>.jar`, so `jar` + relaunch is the whole loop
for this plugin too.

`.\gradlew.bat installSideload` does the same copy on demand, and is the way to force an older
jar back into place (the launcher's copy is timestamp-based and will not overwrite a newer
installed jar).

Notes:

- `compileJava` is **not** enough — it never produces a jar, so the client keeps loading the
  previous build.
- Fully exit RuneLite before rebuilding; a running client holds the sideloaded jar open.
- The client side-loads *every* jar in `sideloaded-plugins`, each under its own class loader,
  and catches failures per jar. This plugin and `ge-helper` are fully independent.

## Rules

1. **Journal**: Keep `JOURNAL.md` updated with all code changes, features added, and decisions
   made.
2. **Rebuild After Changes**: After editing any plugin source or resource, run
   `.\gradlew.bat jar` so the sideloaded jar is current. Never report a change as testable
   in-game without it.
3. **Code Style**: Follow RuneLite plugin conventions — `@Inject`, `@Subscribe`, Lombok
   (`@Slf4j`, `@Getter`), Guice DI, tabs for indentation, braces on their own line.
4. **No automation**: this plugin only ever observes and notifies. Never add anything that
   sends input, queues a menu action, or changes game state.
5. **Fail quiet**: any state the plugin cannot read confidently resolves to `UNKNOWN` and
   produces no alert.
6. **Package**: `com.github.ilee2.prayeralert` — all classes live under this package.
7. **Java Version**: Target Java 11 (RuneLite requirement).
8. **Dependencies**: Only libraries available through RuneLite's client dependency.
