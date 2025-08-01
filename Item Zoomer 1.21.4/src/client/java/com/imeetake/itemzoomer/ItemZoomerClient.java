package com.imeetake.itemzoomer;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;

public class ItemZoomerClient implements ClientModInitializer {
	public static ItemZoomerConfig CONFIG;

	@Override
	public void onInitializeClient() {
		AutoConfig.register(ItemZoomerConfig.class, GsonConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(ItemZoomerConfig.class).getConfig();
	}
}