package com.imeetake.itemzoomer;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;

@Modmenu(modId = "itemzoomer")
@Config(name = "itemzoomer", wrapperName = "ItemZoomerConfig")
public class ItemZoomerConfigModel {
    public boolean enabled = true;
}