package com.logicalsolutions.pvpentityhider;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("pvpautoentityhider")
public interface PvpEntityHiderConfig extends Config
{
	@ConfigItem(
		keyName = "enabled",
		name = "Enable Auto Hider",
		description = "Hide all non-attacker players while under attack in PvP danger areas"
	)
	default boolean enabled()
	{
		return true;
	}

	@Range(
		min = 1,
		max = 15
	)
	@ConfigItem(
		keyName = "gracePeriodSeconds",
		name = "Grace Period (seconds)",
		description = "How long to keep attacker-only hiding active after attacks pause (applied on game ticks, rounded up)"
	)
	default int gracePeriodSeconds()
	{
		return 7;
	}
}
