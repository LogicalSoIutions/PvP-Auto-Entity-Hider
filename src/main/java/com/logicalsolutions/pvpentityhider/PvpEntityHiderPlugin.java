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
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "PvP Auto Entity Hider",
	description = "Hide all players except your attacker while under attack in dangerous PvP areas",
	tags = {"pvp", "entity", "hider", "wilderness"},
	enabledByDefault = false
)
public class PvpEntityHiderPlugin extends Plugin
{
	private static final long MILLIS_PER_SECOND = 1000L;

	@Inject
	private Client client;

	@Inject
	private PvpEntityHiderConfig config;

	@Inject
	private Hooks hooks;

	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;
	private long lastAttackedAtMillis;
	private final Map<String, Long> attackerLastSeenMillis = new HashMap<>();

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

	private boolean shouldDraw(Renderable renderable, boolean drawingUI)
	{
		if (!(renderable instanceof Player))
		{
			return true;
		}

		if (!config.enabled())
		{
			resetAttackState();
			return true;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			resetAttackState();
			return true;
		}

		Player player = (Player) renderable;
		if (player == localPlayer)
		{
			return true;
		}

		if (!isInDangerousPvpArea())
		{
			resetAttackState();
			return true;
		}

		if (!isUnderAttackOrGracePeriod(localPlayer))
		{
			return true;
		}
		long now = System.currentTimeMillis();
		return isActivelyAttackingLocalPlayer(player, localPlayer) || isRecentAttacker(player, now);
	}

	private boolean isInDangerousPvpArea()
	{
		boolean inWilderness = client.getVarbitValue(Varbits.IN_WILDERNESS) == 1;
		boolean hasPvpSpecOrb = client.getVarbitValue(Varbits.PVP_SPEC_ORB) == 1;
		return inWilderness || hasPvpSpecOrb;
	}

	private boolean updateAttackers(Player localPlayer, long now)
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
					attackerLastSeenMillis.put(attackerName, now);
				}
				hasActiveAttacker = true;
			}
		}

		pruneExpiredAttackers(now);
		return hasActiveAttacker;
	}

	private boolean isUnderAttackOrGracePeriod(Player localPlayer)
	{
		long now = System.currentTimeMillis();
		if (updateAttackers(localPlayer, now))
		{
			lastAttackedAtMillis = now;
			return true;
		}

		if (lastAttackedAtMillis == 0L)
		{
			return false;
		}

		long gracePeriodMillis = config.gracePeriodSeconds() * MILLIS_PER_SECOND;
		boolean inGracePeriod = now - lastAttackedAtMillis <= gracePeriodMillis;
		if (!inGracePeriod)
		{
			resetAttackState();
		}
		return inGracePeriod;
	}

	private boolean isRecentAttacker(Player player, long now)
	{
		String name = player.getName();
		if (name == null)
		{
			return false;
		}

		Long lastSeenAt = attackerLastSeenMillis.get(name);
		if (lastSeenAt == null)
		{
			return false;
		}

		long gracePeriodMillis = config.gracePeriodSeconds() * MILLIS_PER_SECOND;
		return now - lastSeenAt <= gracePeriodMillis;
	}

	private void pruneExpiredAttackers(long now)
	{
		long gracePeriodMillis = config.gracePeriodSeconds() * MILLIS_PER_SECOND;
		Iterator<Map.Entry<String, Long>> iterator = attackerLastSeenMillis.entrySet().iterator();
		while (iterator.hasNext())
		{
			Map.Entry<String, Long> entry = iterator.next();
			if (now - entry.getValue() > gracePeriodMillis)
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
		lastAttackedAtMillis = 0L;
		attackerLastSeenMillis.clear();
	}
}
