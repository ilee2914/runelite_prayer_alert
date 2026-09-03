package com.github.ilee2.prayeralert;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(PrayerAlertConfig.GROUP)
public interface PrayerAlertConfig extends Config
{
	String GROUP = "prayeralert";

	@ConfigSection(
		name = "Sound",
		description = "How the alert sounds",
		position = 0
	)
	String soundSection = "sound";

	@ConfigSection(
		name = "When to alert",
		description = "Which prayers and situations trigger the alert",
		position = 1
	)
	String triggerSection = "trigger";

	@ConfigSection(
		name = "Display",
		description = "On-screen warnings",
		position = 2
	)
	String displaySection = "display";

	// ------------------------------------------------------------------ sound

	@ConfigItem(
		keyName = "soundMode",
		name = "Sound",
		description = "Synthesised tone plays through your system at its own volume; game sound effect follows the client's sound settings.",
		position = 0,
		section = soundSection
	)
	default SoundMode soundMode()
	{
		return SoundMode.BEEP;
	}

	@ConfigItem(
		keyName = "tone",
		name = "Tone",
		description = "Which synthesised sound to play. Chime and Marimba are softer; Plain beep is the bare sine driven by the three settings below.",
		position = 1,
		section = soundSection
	)
	default AlertTone tone()
	{
		return AlertTone.CHIME;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = "volume",
		name = "Volume",
		description = "Alert volume. 0 mutes the sound but leaves the on-screen warning working.",
		position = 2,
		section = soundSection
	)
	@Units(Units.PERCENT)
	default int volume()
	{
		return 60;
	}

	@Range(min = 100, max = 2000)
	@ConfigItem(
		keyName = "beepFrequency",
		name = "Beep pitch",
		description = "Plain beep only: frequency of the tone.",
		position = 3,
		section = soundSection
	)
	default int beepFrequency()
	{
		return 880;
	}

	@Range(min = 20, max = 500)
	@ConfigItem(
		keyName = "beepDuration",
		name = "Beep length",
		description = "Plain beep only: length of a single tone.",
		position = 4,
		section = soundSection
	)
	default int beepDuration()
	{
		return 70;
	}

	@Range(min = 1, max = 4)
	@ConfigItem(
		keyName = "beepCount",
		name = "Beeps per alert",
		description = "Plain beep only: how many tones make up one alert.",
		position = 5,
		section = soundSection
	)
	default int beepCount()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "gameSoundId",
		name = "Game sound id",
		description = "Sound effect id used when Sound is set to Game sound effect. 3924 is the Grand Exchange coin tinkle.",
		position = 6,
		section = soundSection
	)
	default int gameSoundId()
	{
		return 3924;
	}

	@Range(min = 1, max = 20)
	@ConfigItem(
		keyName = "repeatTicks",
		name = "Repeat every",
		description = "Game ticks between repeats while the wrong style is still equipped. One tick is 0.6 seconds.",
		position = 7,
		section = soundSection
	)
	@Units(" ticks")
	default int repeatTicks()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "notification",
		name = "Notification",
		description = "Optional tray notification, on top of the sound.",
		position = 8,
		section = soundSection
	)
	default Notification notification()
	{
		return Notification.OFF;
	}

	// ---------------------------------------------------------------- trigger

	@ConfigItem(
		keyName = "alertMelee",
		name = "Protect from Melee",
		description = "Alert when the target protects from melee and you are meleeing it.",
		position = 0,
		section = triggerSection
	)
	default boolean alertMelee()
	{
		return true;
	}

	@ConfigItem(
		keyName = "alertRanged",
		name = "Protect from Missiles",
		description = "Alert when the target protects from missiles and you are attacking with ranged.",
		position = 1,
		section = triggerSection
	)
	default boolean alertRanged()
	{
		return true;
	}

	@ConfigItem(
		keyName = "alertMagic",
		name = "Protect from Magic",
		description = "Alert when the target protects from magic and you are attacking with magic.",
		position = 2,
		section = triggerSection
	)
	default boolean alertMagic()
	{
		return true;
	}

	@ConfigItem(
		keyName = "requireAnimation",
		name = "Only while swinging",
		description = "Require a recent attack animation, so standing next to a target after clicking away goes quiet.",
		position = 3,
		section = triggerSection
	)
	default boolean requireAnimation()
	{
		return true;
	}

	@Range(min = 1, max = 30)
	@ConfigItem(
		keyName = "animationTimeout",
		name = "Swing timeout",
		description = "How long after your last attack animation the alert keeps running.",
		position = 4,
		section = triggerSection
	)
	@Units(" ticks")
	default int animationTimeout()
	{
		return 8;
	}

	@Range(min = 0, max = 20)
	@ConfigItem(
		keyName = "graceTicks",
		name = "Grace period",
		description = "Ticks a mismatch must persist before the first beep. Covers prayer switches you are already reacting to.",
		position = 5,
		section = triggerSection
	)
	@Units(" ticks")
	default int graceTicks()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "ignoredNpcs",
		name = "Ignored NPCs",
		description = "Comma-separated NPC names to never alert on. * matches any run of characters.",
		position = 6,
		section = triggerSection
	)
	default String ignoredNpcs()
	{
		return "";
	}

	// ---------------------------------------------------------------- display

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show warning over target",
		description = "Draw the blocked style above the target while the alert is active.",
		position = 0,
		section = displaySection
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightTarget",
		name = "Tint target",
		description = "Shade the target while the alert is active.",
		position = 1,
		section = displaySection
	)
	default boolean highlightTarget()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "warningColor",
		name = "Warning colour",
		description = "Colour of the on-screen warning.",
		position = 2,
		section = displaySection
	)
	default Color warningColor()
	{
		return new Color(255, 80, 80, 200);
	}

	@ConfigItem(
		keyName = "chatMessage",
		name = "Chat message",
		description = "Print a game message the first time a mismatch starts.",
		position = 3,
		section = displaySection
	)
	default boolean chatMessage()
	{
		return false;
	}

	@ConfigItem(
		keyName = "debugOverlay",
		name = "Debug readout",
		description = "Show the detected weapon category, combat style and target overheads. Useful for reporting a weapon the plugin reads wrong.",
		position = 4,
		section = displaySection
	)
	default boolean debugOverlay()
	{
		return false;
	}

	enum SoundMode
	{
		BEEP("Synthesised tone"),
		GAME_SOUND("Game sound effect"),
		SILENT("None (visual only)");

		private final String name;

		SoundMode(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}
}
