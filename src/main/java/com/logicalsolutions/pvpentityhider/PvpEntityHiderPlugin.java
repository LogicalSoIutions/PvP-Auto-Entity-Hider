package com.logicalsolutions.pvpentityhider;

import com.google.inject.Provides;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Renderable;
import net.runelite.api.Varbits;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "PvP Auto Entity Hider",
	description = "Hide all players except your attacker while under attack in dangerous PvP areas",
	tags = {"pvp", "entity", "hider", "wilderness"}
)
public class PvpEntityHiderPlugin extends Plugin
{
	private static final int MILLIS_PER_SECOND = 1000;
	private static final int MILLIS_PER_TICK = 600;

	@Inject
	private Client client;

	@Inject
	private PvpEntityHiderConfig config;

	@Inject
	private Hooks hooks;

	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;
	private int lastAttackedAtTick;
	private boolean dangerousPvpArea;
	private boolean underAttackOrGracePeriod;
	private final Map<String, Integer> attackerLastSeenTick = new HashMap<>();

	@Override
	protected void startUp()
	{
		resetAttackState();
		hooks.registerRenderableDrawListener(drawListener);
		log.debug("PvP Auto Entity Hider started");
	}

	@Override
	protected void shutDown()
	{
		resetAttackState();
		hooks.unregisterRenderableDrawListener(drawListener);
		log.debug("PvP Auto Entity Hider stopped");
	}

	@Provides
	PvpEntityHiderConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PvpEntityHiderConfig.class);
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		Player localPlayer = client.getLocalPlayer();
		if (!config.enabled() || localPlayer == null)
		{
			resetAttackState();
			return;
		}

		dangerousPvpArea = isInDangerousPvpArea();
		if (!dangerousPvpArea)
		{
			resetAttackState();
			return;
		}

		int tick = client.getTickCount();
		boolean hasActiveAttacker = updateAttackers(localPlayer, tick);
		if (hasActiveAttacker)
		{
			lastAttackedAtTick = tick;
		}

		int gracePeriodTicks = getGracePeriodTicks();
		underAttackOrGracePeriod = hasActiveAttacker
			|| (lastAttackedAtTick >= 0 && tick - lastAttackedAtTick <= gracePeriodTicks);

		if (!underAttackOrGracePeriod)
		{
			resetAttackState();
		}
	}

	private boolean shouldDraw(Renderable renderable, boolean drawingUI)
	{
		if (!(renderable instanceof Player))
		{
			return true;
		}

		if (!config.enabled())
		{
			return true;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return true;
		}

		Player player = (Player) renderable;
		if (player == localPlayer)
		{
			return true;
		}

		if (!dangerousPvpArea || !underAttackOrGracePeriod)
		{
			return true;
		}

		int tick = client.getTickCount();
		return isActivelyAttackingLocalPlayer(player, localPlayer) || isRecentAttacker(player, tick);
	}

	private boolean isInDangerousPvpArea()
	{
		boolean inWilderness = client.getVarbitValue(Varbits.IN_WILDERNESS) == 1;
		boolean hasPvpSpecOrb = client.getVarbitValue(Varbits.PVP_SPEC_ORB) == 1;
		return inWilderness || hasPvpSpecOrb;
	}

	private boolean updateAttackers(Player localPlayer, int tick)
	{
		boolean hasActiveAttacker = false;
		for (Player player : client.getPlayers())
		{
			if (player == null || player == localPlayer)
			{
				continue;
			}

			if (isActivelyAttackingLocalPlayer(player, localPlayer))
			{
				String attackerName = player.getName();
				if (attackerName != null)
				{
					attackerLastSeenTick.put(attackerName, tick);
				}
				hasActiveAttacker = true;
			}
		}

		pruneExpiredAttackers(tick);
		return hasActiveAttacker;
	}

	private int getGracePeriodTicks()
	{
		// Round up so the configured seconds are never shorter than requested.
		return (config.gracePeriodSeconds() * MILLIS_PER_SECOND + (MILLIS_PER_TICK - 1)) / MILLIS_PER_TICK;
	}

	private boolean isRecentAttacker(Player player, int tick)
	{
		String name = player.getName();
		if (name == null)
		{
			return false;
		}

		Integer lastSeenAt = attackerLastSeenTick.get(name);
		if (lastSeenAt == null)
		{
			return false;
		}

		return tick - lastSeenAt <= getGracePeriodTicks();
	}

	private void pruneExpiredAttackers(int tick)
	{
		int gracePeriodTicks = getGracePeriodTicks();
		Iterator<Map.Entry<String, Integer>> iterator = attackerLastSeenTick.entrySet().iterator();
		while (iterator.hasNext())
		{
			Map.Entry<String, Integer> entry = iterator.next();
			if (tick - entry.getValue() > gracePeriodTicks)
			{
				iterator.remove();
			}
		}
	}

	private boolean isActivelyAttackingLocalPlayer(Player player, Player localPlayer)
	{
		return player.getInteracting() == localPlayer && player.getAnimation() != -1;
	}

	private void resetAttackState()
	{
		lastAttackedAtTick = -1;
		dangerousPvpArea = false;
		underAttackOrGracePeriod = false;
		attackerLastSeenTick.clear();
	}
}
