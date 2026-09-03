package com.github.ilee2.prayeralert;

import java.io.ByteArrayInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class AlertToneGeneratorTest
{
	private static final int HEADER_BYTES = 44;

	@Test
	public void everyToneProducesAWavJavaSoundCanRead() throws Exception
	{
		for (AlertTone tone : AlertTone.values())
		{
			final byte[] wav = AlertToneGenerator.generate(tone, 880, 70, 2);

			try (AudioInputStream in = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wav)))
			{
				final AudioFormat format = in.getFormat();
				assertEquals(tone + " sample rate", 44100f, format.getSampleRate(), 0.001f);
				assertEquals(tone + " bit depth", 16, format.getSampleSizeInBits());
				assertEquals(tone + " channels", 1, format.getChannels());
				assertTrue(tone + " should not be empty", in.getFrameLength() > 0);
			}
		}
	}

	@Test
	public void everyToneStartsAndEndsAtSilence()
	{
		for (AlertTone tone : AlertTone.values())
		{
			final byte[] wav = AlertToneGenerator.generate(tone, 880, 70, 2);
			final int samples = (wav.length - HEADER_BYTES) / 2;

			assertEquals(tone + " should start silent", 0, sampleAt(wav, 0));
			assertEquals(tone + " should end silent", 0, sampleAt(wav, samples - 1));
		}
	}

	@Test
	public void everyToneIsNormalisedBelowFullScale()
	{
		for (AlertTone tone : AlertTone.values())
		{
			final byte[] wav = AlertToneGenerator.generate(tone, 880, 70, 2);
			final int samples = (wav.length - HEADER_BYTES) / 2;

			int peak = 0;
			for (int i = 0; i < samples; i++)
			{
				peak = Math.max(peak, Math.abs(sampleAt(wav, i)));
			}

			// Stacking partials must not clip, and every tone must land at one predictable level.
			assertEquals(tone + " peak", (int) (Short.MAX_VALUE * 0.85), peak, 2);
		}
	}

	@Test
	public void synthesisedTonesDecayRatherThanHold()
	{
		for (AlertTone tone : AlertTone.values())
		{
			if (!tone.isSynthesised())
			{
				continue;
			}

			final byte[] wav = AlertToneGenerator.generate(tone, 880, 70, 2);
			final int samples = (wav.length - HEADER_BYTES) / 2;

			// A struck sound is loud early and quiet late; a flat sine would fail this.
			assertTrue(tone + " should decay", peakBetween(wav, 0, samples / 4)
				> peakBetween(wav, samples * 3 / 4, samples) * 2);
		}
	}

	@Test
	public void plainBeepStillHonoursPitchLengthAndCount()
	{
		final byte[] wav = AlertToneGenerator.generate(AlertTone.PLAIN_BEEP, 440, 50, 3);
		final int samples = (wav.length - HEADER_BYTES) / 2;

		// Three 50ms tones separated by two half-length gaps. The gap is toneSamples / 2 in whole
		// samples, so this is 3 * 2205 + 2 * 1102 rather than a round 200ms.
		final int toneSamples = 44100 * 50 / 1000;
		assertEquals(toneSamples * 3 + (toneSamples / 2) * 2, samples);
	}

	private static int peakBetween(byte[] wav, int from, int to)
	{
		int peak = 0;
		for (int i = from; i < to; i++)
		{
			peak = Math.max(peak, Math.abs(sampleAt(wav, i)));
		}
		return peak;
	}

	private static int sampleAt(byte[] wav, int index)
	{
		final int offset = HEADER_BYTES + index * 2;
		return (short) ((wav[offset] & 0xff) | (wav[offset + 1] << 8));
	}
}
