package net.aqualoco.cantinhospooky.network;

import net.aqualoco.cantinhospooky.ClientProxy;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.function.Supplier;

public class PacketHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String PROTOCOL = "1"; // 🔧 Pode versionar sua rede
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("cantinhospooky", "main"), // 🔧 Troque para o ID do seu mod
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );
    private static int ID = 0;

    public static void register() {
        CHANNEL.registerMessage(
                ID++,
                GeradorPacket.class,
                GeradorPacket::encode,
                GeradorPacket::decode,
                (msg, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    if (ctx.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
                        ctx.enqueueWork(() -> {
                            LOGGER.info("[Gerador] pacote recebido no cliente, abrindo tela");
                            ClientProxy.handleOpenGeradorScreen();
                        });
                    }
                    ctx.setPacketHandled(true);
                }
        );

        CHANNEL.registerMessage(
                ID++,
                CritSuccessPacket.class,
                CritSuccessPacket::encode,
                CritSuccessPacket::decode,
                CritSuccessPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }
    public static <MSG> void sendToServer(MSG message) {
        CHANNEL.sendToServer(message);
    }
}
