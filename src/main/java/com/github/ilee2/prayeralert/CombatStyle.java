package com.github.ilee2.prayeralert;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The three damage types an overhead protection prayer can block.
 *
 * <p>{@link #UNKNOWN} means the plugin could not work out what the player is attacking with.
 * Nothing is ever reported as blocked in that case, so an unrecognised weapon produces silence
 * rather than a stream of wrong beeps.
 */
@Getter
@RequiredArgsConstructor
public enum CombatStyle
{
	MELEE("Melee"),
	RANGED("Ranged"),
	MAGIC("Magic"),
	UNKNOWN("Unknown");

	private final String name;
}
