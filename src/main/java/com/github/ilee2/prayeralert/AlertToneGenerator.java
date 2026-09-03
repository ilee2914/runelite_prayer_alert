package com.github.ilee2.prayeralert;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Renders an {@link AlertTone} to an in-memory WAV.
 *
 * <p>Synthesising the alert keeps it configurable and avoids shipping audio files, and unlike
 * {@link net.runelite.api.Client#playSoundEffect} it is unaffected by the in-game sound volume --
 * an alert muted by accident along with the game is worse than no alert.
 */
final class AlertToneGenerator
{
	private static final int SAMPLE_RATE = 44100;
	private static final int BITS_PER_SAMPLE = 16;
	private static final int CHANNELS = 1;
	private static final int HEADER_BYTES = 44;

	/** Linear ramp onto each note, long enough to avoid a click and short enough to stay crisp. */
	private static final int ATTACK_MILLIS = 5;

	/** Ramp applied to the tail of the whole buffer so it always lands on silence. */
	private static final int RELEASE_MILLIS = 12;

	/** Headroom left below full scale after normalising. */
	private static final double PEAK = 0.85;

	private AlertToneGenerator()
	{
	}

	/**
	 * @param tone which sound to render
	 * @param beepFrequencyHz pitch, {@link AlertTone#PLAIN_BEEP} only
	 * @param beepDurationMillis length of one tone, {@link AlertTone#PLAIN_BEEP} only
	 * @param beepCount tones per alert, {@link AlertTone#PLAIN_BEEP} only
	 * @return a complete WAV file
	 */
	static byte[] generate(AlertTone tone, int beepFrequencyHz, int beepDurationMillis, int beepCount)
	{
		final double[] samples = tone.isSynthesised()
			? renderTone(tone)
			: renderPlainBeep(beepFrequencyHz, beepDurationMillis, beepCount);

		applyRelease(samples);
		normalise(samples);

		return toWav(samples);
	}

	/** Additive synthesis: each note is a stack of partials under an exponential decay. */
	private static double[] renderTone(AlertTone tone)
	{
		final double[] notes = tone.getNotes();
		final double[][] partials = tone.getPartials();

		final int offsetSamples = millisToSamples(tone.getNoteOffsetMillis());
		final int total = millisToSamples(tone.getTotalMillis());
		final double tau = tone.getDecayMillis() / 1000.0;
		final int attackSamples = millisToSamples(ATTACK_MILLIS);

		final double[] samples = new double[total];

		for (int note = 0; note < notes.length; note++)
		{
			final int start = note * offsetSamples;

			for (int i = 0; start + i < total; i++)
			{
				final double t = (double) i / SAMPLE_RATE;

				double value = 0.0;
				for (double[] partial : partials)
				{
					value += partial[1] * Math.sin(2.0 * Math.PI * notes[note] * partial[0] * t);
				}

				final double attack = i < attackSamples ? (double) i / attackSamples : 1.0;
				samples[start + i] += value * attack * Math.exp(-t / tau);
			}
		}

		return samples;
	}

	/** The original tone: a flat-envelope sine, repeated, with a short fade on each end. */
	private static double[] renderPlainBeep(int frequencyHz, int durationMillis, int count)
	{
		final int toneSamples = Math.max(1, millisToSamples(durationMillis));
		final int gapSamples = toneSamples / 2;
		final double[] samples = new double[toneSamples * count + gapSamples * Math.max(0, count - 1)];

		final int fadeSamples = Math.min(toneSamples / 2, millisToSamples(ATTACK_MILLIS));

		int at = 0;
		for (int tone = 0; tone < count; tone++)
		{
			for (int i = 0; i < toneSamples; i++)
			{
				final double angle = 2.0 * Math.PI * frequencyHz * i / SAMPLE_RATE;
				samples[at + i] = Math.sin(angle) * fade(i, toneSamples, fadeSamples);
			}

			at += toneSamples + gapSamples;
		}

		return samples;
	}

	private static double fade(int sample, int length, int fadeSamples)
	{
		if (fadeSamples <= 0)
		{
			return 1.0;
		}

		if (sample < fadeSamples)
		{
			return (double) sample / fadeSamples;
		}

		final int fromEnd = length - 1 - sample;
		return fromEnd < fadeSamples ? (double) fromEnd / fadeSamples : 1.0;
	}

	private static void applyRelease(double[] samples)
	{
		final int release = Math.min(samples.length, millisToSamples(RELEASE_MILLIS));
		for (int i = 0; i < release; i++)
		{
			final int index = samples.length - release + i;
			// (i + 1) / release, so the final sample is scaled to exactly zero. Stopping one step
			// short leaves a step down to silence, which is the click the ramp exists to prevent.
			samples[index] *= 1.0 - (double) (i + 1) / release;
		}
	}

	/** Scales to a fixed peak, so stacking partials cannot clip and every tone lands at one level. */
	private static void normalise(double[] samples)
	{
		double peak = 0.0;
		for (double sample : samples)
		{
			peak = Math.max(peak, Math.abs(sample));
		}

		if (peak <= 0.0)
		{
			return;
		}

		final double scale = PEAK / peak;
		for (int i = 0; i < samples.length; i++)
		{
			samples[i] *= scale;
		}
	}

	private static byte[] toWav(double[] samples)
	{
		final int dataBytes = samples.length * CHANNELS * (BITS_PER_SAMPLE / 8);
		final ByteBuffer buffer = ByteBuffer.allocate(HEADER_BYTES + dataBytes).order(ByteOrder.LITTLE_ENDIAN);

		writeHeader(buffer, dataBytes);

		for (double sample : samples)
		{
			buffer.putShort((short) Math.round(sample * Short.MAX_VALUE));
		}

		return buffer.array();
	}

	private static void writeHeader(ByteBuffer buffer, int dataBytes)
	{
		final int byteRate = SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8);
		final int blockAlign = CHANNELS * (BITS_PER_SAMPLE / 8);

		buffer.put("RIFF".getBytes(StandardCharsets.US_ASCII));
		buffer.putInt(36 + dataBytes);
		buffer.put("WAVE".getBytes(StandardCharsets.US_ASCII));

		buffer.put("fmt ".getBytes(StandardCharsets.US_ASCII));
		buffer.putInt(16);              // subchunk size
		buffer.putShort((short) 1);     // PCM
		buffer.putShort((short) CHANNELS);
		buffer.putInt(SAMPLE_RATE);
		buffer.putInt(byteRate);
		buffer.putShort((short) blockAlign);
		buffer.putShort((short) BITS_PER_SAMPLE);

		buffer.put("data".getBytes(StandardCharsets.US_ASCII));
		buffer.putInt(dataBytes);
	}

	private static int millisToSamples(int millis)
	{
		return SAMPLE_RATE * millis / 1000;
	}
}
