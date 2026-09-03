package com.github.ilee2.prayeralert;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.Getter;

/**
 * The overhead prayer icons an NPC can display, and which combat styles each one protects
 * against.
 *
 * <p>Ordinals are the sprite indexes inside the game's overhead-prayer sprite archive, which is
 * also the ordering of {@link net.runelite.api.HeadIcon} (that enum is only exposed for players --
 * NPCs hand out raw archive/sprite id pairs, see {@link OverheadPrayerReader}).
 */
@Getter
public enum OverheadPrayer
{
	PROTECT_FROM_MELEE("Protect from Melee", CombatStyle.MELEE),
	PROTECT_FROM_MISSILES("Protect from Missiles", CombatStyle.RANGED),
	PROTECT_FROM_MAGIC("Protect from Magic", CombatStyle.MAGIC),
	RETRIBUTION("Retribution"),
	SMITE("Smite"),
	REDEMPTION("Redemption"),
	PROTECT_RANGE_MAGE("Protect from Missiles + Magic", CombatStyle.RANGED, CombatStyle.MAGIC),
	PROTECT_RANGE_MELEE("Protect from Missiles + Melee", CombatStyle.RANGED, CombatStyle.MELEE),
	PROTECT_MAGE_MELEE("Protect from Magic + Melee", CombatStyle.MAGIC, CombatStyle.MELEE),
	PROTECT_ALL("Protect from All", CombatStyle.RANGED, CombatStyle.MAGIC, CombatStyle.MELEE),
	WRATH("Wrath"),
	SOUL_SPLIT("Soul Split"),
	DEFLECT_MELEE("Deflect Melee", CombatStyle.MELEE),
	DEFLECT_MISSILES("Deflect Missiles", CombatStyle.RANGED),
	DEFLECT_MAGIC("Deflect Magic", CombatStyle.MAGIC),
	;

	private static final OverheadPrayer[] VALUES = values();

	private final String name;
	private final Set<CombatStyle> blockedStyles;

	OverheadPrayer(String name, CombatStyle... blockedStyles)
	{
		this.name = name;
		this.blockedStyles = blockedStyles.length == 0
			? Collections.emptySet()
			: Collections.unmodifiableSet(EnumSet.of(blockedStyles[0], blockedStyles));
	}

	/**
	 * @return the prayer at the given sprite index, or null if the index is outside the archive
	 * (a game update adding new icons lands here, and is treated as "no protection" rather than
	 * guessed at).
	 */
	@Nullable
	static OverheadPrayer forSpriteIndex(int spriteIndex)
	{
		return spriteIndex >= 0 && spriteIndex < VALUES.length ? VALUES[spriteIndex] : null;
	}

	boolean blocks(CombatStyle style)
	{
		return blockedStyles.contains(style);
	}

	boolean isProtective()
	{
		return !blockedStyles.isEmpty();
	}
}
