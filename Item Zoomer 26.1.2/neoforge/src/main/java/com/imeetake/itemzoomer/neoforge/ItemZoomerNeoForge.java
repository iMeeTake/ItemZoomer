package com.imeetake.itemzoomer.neoforge;

import com.imeetake.itemzoomer.ItemZoomer;
import com.imeetake.itemzoomer.config.EnumCycleProvider;
import com.imeetake.itemzoomer.config.ItemZoomerConfig;
import com.imeetake.itemzoomer.render.ZoomedItemPIPRenderer;
import com.imeetake.itemzoomer.render.ZoomedItemRenderState;
import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.autoconfig.AutoConfigClient;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.lwjgl.glfw.GLFW;

@Mod(ItemZoomer.MOD_ID)
public final class ItemZoomerNeoForge {

    public ItemZoomerNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            ClientInit.init(modEventBus, modContainer);
        }
    }

    public static final class ClientInit {
        private static KeyMapping toggleKey;

        private static void init(IEventBus modEventBus, ModContainer modContainer) {
            ItemZoomer.init();
            registerConfigGui();

            modEventBus.addListener(ClientInit::onRegisterPIPRenderers);
            modEventBus.addListener(ClientInit::onRegisterKeyMappings);

            modContainer.registerExtensionPoint(
                    IConfigScreenFactory.class,
                    (container, parent) -> AutoConfigClient.getConfigScreen(ItemZoomerConfig.class, parent).get()
            );
        }

        private static void registerConfigGui() {
            GuiRegistry registry = AutoConfigClient.getGuiRegistry(ItemZoomerConfig.class);
            registry.registerPredicateProvider(new EnumCycleProvider(), field -> field.getType().isEnum());
        }

        public static boolean matchesToggle(KeyEvent keyEvent) {
            return toggleKey != null && toggleKey.matches(keyEvent);
        }

        private static void onRegisterPIPRenderers(RegisterPictureInPictureRenderersEvent event) {
            event.register(ZoomedItemRenderState.class, ZoomedItemPIPRenderer::new);
        }

        private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            toggleKey = new KeyMapping(
                    "key.itemzoomer.toggle_zoom",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_Z,
                    KeyMapping.Category.register(Identifier.fromNamespaceAndPath("itemzoomer", "category"))
            );

            event.register(toggleKey);
        }
    }
}
