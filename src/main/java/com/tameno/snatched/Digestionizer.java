package com.tameno.snatched;

import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;

public class Digestionizer {
	private final HashMap<PlayerEntity, Integer> table = new HashMap<>();

	public void applyOnce() {
		for (PlayerEntity player : table.keySet()) {
			Integer tagetHunger = table.get(player);
			if (tagetHunger == null || tagetHunger < 1) {
				table.remove(player);
				continue;
			}
			int currentHunger = player.getHungerManager().getFoodLevel();
			if (currentHunger == 20) {
				continue;
			}
			tagetHunger -= 1;
			player.getHungerManager().add(1, 20);
			table.put(player, tagetHunger);
		}
	}

	public void add(PlayerEntity player, int targetFoodLevel) {
		table.put(player, targetFoodLevel);
	}

	public boolean has(PlayerEntity player) {
		return table.containsKey(player);
	}

	public boolean remove(PlayerEntity player) {
		return table.remove(player) != null;
	}
}
