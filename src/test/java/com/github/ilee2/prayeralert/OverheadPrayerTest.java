package com.github.ilee2.prayeralert;

import java.util.EnumSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class OverheadPrayerTest
{
	@Test
	public void spriteIndexesMatchPrayerArchiveOrder()
	{
		assertEquals(OverheadPrayer.PROTECT_FROM_MELEE, OverheadPrayer.forSpriteIndex(0));
		assertEquals(OverheadPrayer.PROTECT_FROM_MISSILES, OverheadPrayer.forSpriteIndex(1));
		assertEquals(OverheadPrayer.PROTECT_FROM_MAGIC, OverheadPrayer.forSpriteIndex(2));
		assertEquals(OverheadPrayer.PROTECT_ALL, OverheadPrayer.forSpriteIndex(9));
		assertEquals(OverheadPrayer.DEFLECT_MAGIC, OverheadPrayer.forSpriteIndex(14));
	}

	@Test
	public void unknownSpriteIndexIsNotGuessedAt()
	{
		assertNull(OverheadPrayer.forSpriteIndex(-1));
		assertNull(OverheadPrayer.forSpriteIndex(15));
	}

	@Test
	public void offensivePrayersBlockNothing()
	{
		assertFalse(OverheadPrayer.RETRIBUTION.isProtective());
		assertFalse(OverheadPrayer.SMITE.isProtective());
		assertFalse(OverheadPrayer.REDEMPTION.isProtective());
		assertFalse(OverheadPrayer.SOUL_SPLIT.isProtective());
		assertFalse(OverheadPrayer.WRATH.isProtective());
	}

	@Test
	public void combinedIconsBlockEveryStyleTheyShow()
	{
		assertTrue(OverheadPrayer.PROTECT_MAGE_MELEE.blocks(CombatStyle.MAGIC));
		assertTrue(OverheadPrayer.PROTECT_MAGE_MELEE.blocks(CombatStyle.MELEE));
		assertFalse(OverheadPrayer.PROTECT_MAGE_MELEE.blocks(CombatStyle.RANGED));

		assertEquals(EnumSet.of(CombatStyle.MELEE, CombatStyle.RANGED, CombatStyle.MAGIC),
			OverheadPrayer.PROTECT_ALL.getBlockedStyles());
	}

	@Test
	public void deflectIconsBlockLikeProtectIcons()
	{
		assertTrue(OverheadPrayer.DEFLECT_MELEE.blocks(CombatStyle.MELEE));
		assertTrue(OverheadPrayer.DEFLECT_MISSILES.blocks(CombatStyle.RANGED));
		assertTrue(OverheadPrayer.DEFLECT_MAGIC.blocks(CombatStyle.MAGIC));
	}

	@Test
	public void multipleOverheadsAreUnioned()
	{
		final Set<OverheadPrayer> overheads = EnumSet.of(
			OverheadPrayer.PROTECT_FROM_MELEE, OverheadPrayer.PROTECT_FROM_MAGIC, OverheadPrayer.SMITE);

		assertEquals(EnumSet.of(CombatStyle.MELEE, CombatStyle.MAGIC),
			OverheadPrayerReader.blockedStyles(overheads));
	}
}
