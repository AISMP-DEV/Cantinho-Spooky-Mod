// ClientProxy.java
package net.aqualoco.cantinhospooky;

import net.aqualoco.cantinhospooky.client.screen.GeradorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientProxy extends CommonProxy {
    private static final Logger LOGGER = LogManager.getLogger();
    public static void handleOpenGeradorScreen() {
        LOGGER.info("[Gerador] handleOpenGeradorScreen() chamado");
        Minecraft.getInstance().setScreen(
                new GeradorScreen(Component.literal("Gerador"))
        );
    }
}
