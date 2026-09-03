package com.github.ilee2.prayeralert;

import com.google.inject.Provides;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.Notifier;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * Sounds an alert while the player attacks an NPC with a combat style that NPC's overhead prayer
 * protects against.
 */
@Slf4j
@PluginDescriptor(
	name = "Prayer Alert",
	description = "Beeps while you attack an NPC with a style its overhead prayer blocks",
	tags = {"prayer", "overhead", "protect", "combat", "style", "sound", "accessibility"}
)
public class PrayerAlertPlugin extends Plugin
{
	/**
	 * How long a manual spell cast counts as "I am attacking with magic". A cast takes effect
	 * within a few ticks and is not repeated automatically, so this only has to outlive the
	 * projectile.
	 */
	private static final int MANUAL_CAST_TICKS = 5;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PrayerAlertConfig config;

	@Inject
	private AttackStyleResolver attackStyleResolver;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PrayerAlertOverlay overlay;

	@Inject
	private AudioPlayer audioPlayer;

	@Inject
	private Notifier notifier;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ScheduledExecutorService executor;

	/** The NPC the player is attacking, whether or not anything is wrong. For the debug readout. */
	@Getter
	@Nullable
	private NPC currentTarget;

	/** Overheads on {@link #currentTarget}. */
	@Getter
	private Set<OverheadPrayer> currentTargetPrayers = Collections.emptySet();

	/** The NPC currently being warned about, or null when nothing is wrong. */
	@Getter
	@Nullable
	private NPC blockedTarget;

	@Getter
	private CombatStyle currentStyle = CombatStyle.UNKNOWN;

	private List<Pattern> ignoredNpcPatterns = Collections.emptyList();

	private byte[] beepWav;

	private int mismatchSinceTick = -1;
	private int lastAlertTick = -1;
	private int lastAttackAnimationTick = -1;
	private int manualCastUntilTick = -1;

	@Provides
	PrayerAlertConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PrayerAlertConfig.class);
	}

	@Override
	protected void startUp()
	{
		rebuildIgnoredNpcs();
		rebuildTone();
		overlayManager.add(overlay);
		clientThread.invokeLater(attackStyleResolver::update);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		attackStyleResolver.reset();
		clearAlert();
		beepWav = null;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!PrayerAlertConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		rebuildIgnoredNpcs();
		rebuildTone();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			attackStyleResolver.reset();
			currentTarget = null;
			currentTargetPrayers = Collections.emptySet();
			clearAlert();
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		final Player local = client.getLocalPlayer();
		if (local == null || event.getActor() != local)
		{
			return;
		}

		// Anything that animates the player while they are locked onto an NPC is an attack in
		// practice -- skilling animations do not run against an NPC target. Catching it on the
		// event as well as on the tick picks up animations shorter than a tick.
		if (local.getAnimation() != -1 && local.getInteracting() instanceof NPC)
		{
			lastAttackAnimationTick = client.getTickCount();
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		// A spell cast by hand does not change the attack style varp, so a melee weapon plus a
		// manual barrage would otherwise read as melee.
		if (event.getMenuAction() == MenuAction.WIDGET_TARGET_ON_NPC
			&& "Cast".equals(event.getMenuOption()))
		{
			manualCastUntilTick = client.getTickCount() + MANUAL_CAST_TICKS;
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		attackStyleResolver.update();
		currentStyle = resolveCurrentStyle();

		final NPC target = attackedNpc();
		currentTarget = target;
		currentTargetPrayers = target == null ? Collections.emptySet() : OverheadPrayerReader.read(target);

		if (target == null)
		{
			clearAlert();
			return;
		}

		final OverheadPrayer blocking = blockingPrayer(currentTargetPrayers, currentStyle);
		if (blocking == null)
		{
			clearAlert();
			return;
		}

		final int now = client.getTickCount();
		if (blockedTarget != target)
		{
			// A different NPC, or the first tick of this mismatch: start the grace period over.
			mismatchSinceTick = now;
			lastAlertTick = -1;
		}

		blockedTarget = target;

		if (now - mismatchSinceTick < config.graceTicks())
		{
			return;
		}

		if (lastAlertTick < 0 || now - lastAlertTick >= config.repeatTicks())
		{
			alert(target, blocking, lastAlertTick < 0);
			lastAlertTick = now;
		}
	}

	/** @return the NPC the player is attacking and should be warned about, or null. */
	@Nullable
	private NPC attackedNpc()
	{
		final Player local = client.getLocalPlayer();
		if (local == null)
		{
			return null;
		}

		final Actor interacting = local.getInteracting();
		if (!(interacting instanceof NPC))
		{
			return null;
		}

		final NPC npc = (NPC) interacting;
		if (npc.isDead() || npc.getHealthRatio() == 0)
		{
			return null;
		}

		// Sampling the animation here as well as on the event covers one that is held across ticks
		// without re-firing.
		if (local.getAnimation() != -1)
		{
			lastAttackAnimationTick = client.getTickCount();
		}

		if (config.requireAnimation()
			&& client.getTickCount() - lastAttackAnimationTick > config.animationTimeout())
		{
			return null;
		}

		return isIgnored(npc) ? null : npc;
	}

	/** @return the overhead blocking {@code style} and enabled in config, or null if none is. */
	@Nullable
	private OverheadPrayer blockingPrayer(Set<OverheadPrayer> prayers, CombatStyle style)
	{
		if (style == CombatStyle.UNKNOWN || !isAlertEnabled(style))
		{
			return null;
		}

		for (OverheadPrayer prayer : prayers)
		{
			if (prayer.blocks(style))
			{
				return prayer;
			}
		}

		return null;
	}

	private boolean isAlertEnabled(CombatStyle style)
	{
		switch (style)
		{
			case MELEE:
				return config.alertMelee();
			case RANGED:
				return config.alertRanged();
			case MAGIC:
				return config.alertMagic();
			default:
				return false;
		}
	}

	private CombatStyle resolveCurrentStyle()
	{
		if (client.getTickCount() <= manualCastUntilTick)
		{
			return CombatStyle.MAGIC;
		}

		return attackStyleResolver.getCurrentStyle();
	}

	/** @param first whether this is the opening alert of a mismatch, rather than a repeat. */
	private void alert(NPC target, OverheadPrayer prayer, boolean first)
	{
		playSound();

		if (!first)
		{
			// The sound repeats; the chat line and the tray notification do not.
			return;
		}

		if (config.chatMessage())
		{
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(String.format("[Prayer Alert] %s is using %s - %s is blocked.",
					target.getName(), prayer.getName(), currentStyle.getName()))
				.build());
		}

		notifier.notify(config.notification(),
			String.format("%s blocks %s", target.getName(), currentStyle.getName()));
	}

	private void playSound()
	{
		final int volume = config.volume();
		if (volume <= 0)
		{
			return;
		}

		switch (config.soundMode())
		{
			case GAME_SOUND:
				// Scaled to the client's 0-127 sound effect volume.
				client.playSoundEffect(config.gameSoundId(), volume * 127 / 100);
				break;

			case BEEP:
				playBeep(volume);
				break;

			case SILENT:
			default:
				break;
		}
	}

	private void playBeep(int volume)
	{
		final byte[] wav = beepWav;
		if (wav == null)
		{
			return;
		}

		// Percent -> decibels of attenuation, which is what AudioPlayer's gain control expects.
		final float gainDb = (float) (20.0 * Math.log10(volume / 100.0));

		executor.execute(() ->
		{
			try
			{
				audioPlayer.play(new ByteArrayInputStream(wav), gainDb);
			}
			catch (Exception e)
			{
				log.warn("Unable to play prayer alert beep", e);
			}
		});
	}

	private void clearAlert()
	{
		blockedTarget = null;
		mismatchSinceTick = -1;
		lastAlertTick = -1;
	}

	private void rebuildTone()
	{
		beepWav = AlertToneGenerator.generate(config.tone(), config.beepFrequency(), config.beepDuration(),
			config.beepCount());
	}

	private void rebuildIgnoredNpcs()
	{
		final String raw = config.ignoredNpcs();
		if (raw == null || raw.trim().isEmpty())
		{
			ignoredNpcPatterns = Collections.emptyList();
			return;
		}

		final List<Pattern> patterns = new ArrayList<>();
		for (String entry : raw.split(","))
		{
			final String trimmed = entry.trim();
			if (trimmed.isEmpty())
			{
				continue;
			}

			final StringBuilder regex = new StringBuilder();
			for (String literal : trimmed.split("\\*", -1))
			{
				if (regex.length() > 0)
				{
					regex.append(".*");
				}
				regex.append(Pattern.quote(literal));
			}

			patterns.add(Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE));
		}

		ignoredNpcPatterns = patterns;
	}

	private boolean isIgnored(NPC npc)
	{
		if (ignoredNpcPatterns.isEmpty())
		{
			return false;
		}

		final String name = npc.getName();
		if (name == null)
		{
			return false;
		}

		final String plain = name.replaceAll("<[^>]*>", "").toLowerCase(Locale.ROOT);
		for (Pattern pattern : ignoredNpcPatterns)
		{
			if (pattern.matcher(plain).matches())
			{
				return true;
			}
		}

		return false;
	}

	int getWeaponCategory()
	{
		return attackStyleResolver.getWeaponCategory();
	}
}
