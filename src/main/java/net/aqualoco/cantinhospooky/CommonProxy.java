// CommonProxy.java
package net.aqualoco.cantinhospooky;

import net.aqualoco.cantinhospooky.network.PacketHandler;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class CommonProxy {
    public void setup(final FMLCommonSetupEvent event) {
        PacketHandler.register();
    }
}
