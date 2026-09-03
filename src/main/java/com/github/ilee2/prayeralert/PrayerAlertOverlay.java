package com.github.ilee2.prayeralert;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.Set;
import java.util.StringJoiner;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Draws the warning over the target while the alert is active, plus an optional readout of what
 * the plugin thinks is going on.
 */
class PrayerAlertOverlay extends Overlay
{
	private final Client client;
	private final PrayerAlertPlugin plugin;
	private final PrayerAlertConfig config;

	@Inject
	PrayerAlertOverlay(Client client, PrayerAlertPlugin plugin, PrayerAlertConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final NPC target = plugin.getBlockedTarget();

		if (target != null)
		{
			if (config.highlightTarget())
			{
				final Shape hull = target.getConvexHull();
				if (hull != null)
				{
					OverlayUtil.renderPolygon(graphics, hull, config.warningColor());
				}
			}

			if (config.showOverlay())
			{
				renderWarningText(graphics, target);
			}
		}

		if (config.debugOverlay())
		{
			renderDebug(graphics, target != null);
		}

		return null;
	}

	private void renderWarningText(Graphics2D graphics, NPC target)
	{
		final String text = plugin.getCurrentStyle().getName() + " blocked";
		final Point location = target.getCanvasTextLocation(graphics, text, target.getLogicalHeight() + 40);
		if (location != null)
		{
			OverlayUtil.renderTextLocation(graphics, location, text, config.warningColor());
		}
	}

	private void renderDebug(Graphics2D graphics, boolean blocked)
	{
		final NPC target = plugin.getCurrentTarget();
		final Set<OverheadPrayer> prayers = plugin.getCurrentTargetPrayers();

		final StringJoiner prayerNames = new StringJoiner(", ");
		for (OverheadPrayer prayer : prayers)
		{
			prayerNames.add(prayer.getName());
		}

		final String text = String.format("style=%s cat=%d target=%s overheads=[%s]",
			plugin.getCurrentStyle().getName(),
			plugin.getWeaponCategory(),
			target == null ? "-" : target.getName(),
			prayers.isEmpty() ? "-" : prayerNames.toString());

		// Anchored to the viewport rather than the target so it stays put when nothing is wrong.
		final int x = client.getViewportXOffset() + 6;
		final int y = client.getViewportYOffset() + client.getViewportHeight() - 8;

		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x + 1, y + 1);
		graphics.setColor(blocked ? config.warningColor() : Color.WHITE);
		graphics.drawString(text, x, y);
	}
}
