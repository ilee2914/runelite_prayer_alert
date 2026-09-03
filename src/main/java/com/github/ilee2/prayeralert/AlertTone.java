package com.github.ilee2.prayeralert;

import javax.annotation.Nullable;
import lombok.Getter;

/**
 * Shapes of alert tone.
 *
 * <p>What makes a repeating alert grating is mostly timbre and envelope, not volume: a bare sine
 * held at constant amplitude has no attack and no decay, so it reads as a smoke-alarm chirp. Each
 * tone here is additive synthesis — a fundamental plus a few partials — under an exponential decay,
 * which is what gives struck instruments their "pleasant but noticeable" character. Two notes a
 * consonant interval apart make it read as a signal rather than a fault.
 */
@Getter
public enum AlertTone
{
	/** Two-note rising fifth with bell partials. Soft attack, long ring. */
	CHIME("Chime",
		new double[]{880.00, 1318.51},                                  // A5 -> E6
		130,
		new double[][]{{1.0, 1.00}, {2.0, 0.40}, {3.0, 0.16}, {4.2, 0.07}},
		150,
		560),

	/** Warm wooden double tap. Marimba bars are tuned to their 4th partial, hence the 4.0 ratio. */
	MARIMBA("Marimba",
		new double[]{659.25, 987.77},                                   // E5 -> B5
		120,
		new double[][]{{1.0, 1.00}, {4.0, 0.35}, {10.0, 0.06}},
		95,
		420),

	/** One short, round note. The most understated option. */
	SOFT_BLIP("Soft blip",
		new double[]{659.25},                                           // E5
		0,
		new double[][]{{1.0, 1.00}, {2.0, 0.14}},
		110,
		260),

	/** The original flat-envelope sine, driven by the pitch/length/count settings. */
	PLAIN_BEEP("Plain beep", null, 0, null, 0, 0),
	;

	private final String name;

	/** Note frequencies in Hz, played in order. Null for {@link #PLAIN_BEEP}. */
	@Nullable
	private final double[] notes;

	/** Milliseconds between the onset of one note and the next. */
	private final int noteOffsetMillis;

	/** {@code {harmonic ratio, amplitude}} pairs summed to build the timbre. */
	@Nullable
	private final double[][] partials;

	/** Time constant of the exponential decay, in milliseconds. Lower is more percussive. */
	private final int decayMillis;

	/** Total length of one alert. */
	private final int totalMillis;

	AlertTone(String name, @Nullable double[] notes, int noteOffsetMillis, @Nullable double[][] partials,
		int decayMillis, int totalMillis)
	{
		this.name = name;
		this.notes = notes;
		this.noteOffsetMillis = noteOffsetMillis;
		this.partials = partials;
		this.decayMillis = decayMillis;
		this.totalMillis = totalMillis;
	}

	boolean isSynthesised()
	{
		return notes != null && partials != null;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
