package com.logicalsolutions.pvpentityhider;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PvpEntityHiderPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PvpEntityHiderPlugin.class);
		RuneLite.main(args);
	}
}
