package net.aqualoco.cantinhospooky.network;

import net.aqualoco.cantinhospooky.CantinhoSpooky; // Para ResourceLocation
import net.aqualoco.cantinhospooky.ClientProxy;    // Import necessário
import net.minecraft.core.BlockPos;             // Import necessário
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection; // Import necessário
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;                         // Import Logger            // Import LogManager se usar o logger daqui
import net.minecraftforge.fml.LogicalSide;      // Import LogicalSide

import java.util.Optional;

public class PacketHandler {
    // Usar o Logger do seu mod principal ou criar um novo
    private static final Logger LOGGER = CantinhoSpooky.LOGGER; // Ou LogManager.getLogger();
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CantinhoSpooky.MOD_ID, "main"), // Usando MOD_ID da classe principal
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );
    private static int ID = 0;

    public static void register() {

        // --- Pacote: GeradorPacket (S -> C) ---
        CHANNEL.registerMessage(
                ID++,
                GeradorPacket.class,
                GeradorPacket::encode, // Já atualizado para incluir BlockPos
                GeradorPacket::decode, // Já atualizado para incluir BlockPos
                (msg, ctxSupplier) -> { // Handler no Cliente
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    // Verificação de lado é boa prática, embora enqueueWork geralmente só funcione no cliente aqui
                    if (ctx.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
                        ctx.enqueueWork(() -> {
                            // ---> ALTERAÇÃO AQUI <---
                            // Extrai a BlockPos do pacote e passa para o método que abre a tela
                            BlockPos blockPos = msg.pos();
                            LOGGER.debug("[Gerador] Pacote GeradorPacket recebido no cliente para {}, abrindo tela", blockPos);
                            // Assumindo que handleOpenGeradorScreen agora aceita BlockPos (faremos isso depois)
                            ClientProxy.handleOpenGeradorScreen(blockPos);
                        });
                    } else {
                        LOGGER.warn("GeradorPacket recebido no lado inesperado: {}", ctx.getDirection().getReceptionSide());
                    }
                    ctx.setPacketHandled(true);
                },
                // Adiciona direção explícita para S -> C (boa prática)
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        // --- Pacote: CritSuccessPacket (C -> S) ---
        CHANNEL.registerMessage(
                ID++,
                CritSuccessPacket.class,
                CritSuccessPacket::encode, // Já atualizado para incluir BlockPos
                CritSuccessPacket::decode, // Já atualizado para incluir BlockPos
                CritSuccessPacket::handle, // O handler estático já foi atualizado para usar BlockPos
                Optional.of(NetworkDirection.PLAY_TO_SERVER) // Direção C -> S
        );

        // --- Pacote: FailResultPacket (C -> S) ---
        // ---> NOVO REGISTRO AQUI <---
        CHANNEL.registerMessage(
                ID++,
                FailResultPacket.class, // A classe que acabamos de criar
                FailResultPacket::encode, // Método encode (com BlockPos)
                FailResultPacket::decode, // Método decode (com BlockPos)
                FailResultPacket::handle, // Método handle estático (que chama startGlobalCooldown no BE)
                Optional.of(NetworkDirection.PLAY_TO_SERVER) // Direção C -> S
        );

        LOGGER.info("Pacotes do CantinhoSpooky registrados.");
    }

    // Método sendToServer permanece o mesmo
    public static <MSG> void sendToServer(MSG message) {
        CHANNEL.sendToServer(message);
    }

    // Poderíamos adicionar um sendToPlayer se necessário
    // public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
    //    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    // }
}