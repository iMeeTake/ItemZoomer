package com.imeetake.itemzoomer.accessor;

import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

public interface ContainerScreenAccessor {

    @Nullable
    Slot itemzoomer$getHoveredSlot();

    int itemzoomer$getLeftPos();

    int itemzoomer$getTopPos();

    int itemzoomer$getImageWidth();

    int itemzoomer$getImageHeight();
}