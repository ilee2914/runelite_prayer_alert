package com.github.ilee2.prayeralert;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PrayerAlertPluginTest
{
	public static void main(String[] args) throws Exception
	{
		// Add all your private plugin classes here to load them together
		ExternalPluginManager.loadBuiltin(PrayerAlertPlugin.class);
		RuneLite.main(args);
	}
}
