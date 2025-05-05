package net.aqualoco.cantinhospooky.network;

import net.aqualoco.cantinhospooky.CantinhoSpooky; // Para o LOGGER
import net.aqualoco.cantinhospooky.block.entity.GeradorBlockEntity; // Importar o BE
import net.minecraft.core.BlockPos;             // Importar BlockPos
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component; // Para mensagens futuras
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity; // Importar BlockEntity base
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.function.Supplier;

public class FailResultPacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    // 1. Adicionar o campo BlockPos
    private final BlockPos pos;

    // 2. Criar o construtor que aceita BlockPos
    public FailResultPacket(BlockPos pos) {
        this.pos = pos;
    }

    // 3. Encode: Escrever a BlockPos
    public static void encode(FailResultPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    // 4. Decode: Ler a BlockPos e criar o pacote
    public static FailResultPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new FailResultPacket(pos);
    }

    // 5. Handle: Lógica no servidor para processar a falha
    public static void handle(FailResultPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ServerPlayer player = ctx.getSender(); // Jogador que enviou o pacote (quem falhou)
        if (player == null) {
            LOGGER.error("[CantinhoSpooky] Erro: FailResultPacket recebido sem remetente.");
            ctx.setPacketHandled(true);
            return;
        }

        ctx.enqueueWork(() -> { // Roda na thread principal do servidor
            ServerLevel level = player.serverLevel();
            BlockPos pos = msg.pos; // Posição do gerador vinda do pacote

            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof GeradorBlockEntity geradorBE) {
                // Verifica se o bloco realmente precisava de reparo (evita cooldown desnecessário)
                if (geradorBE.needsRepair()) {
                    LOGGER.info("Jogador {} falhou no reparo do Gerador em {}", player.getName().getString(), pos);

                    // Chama o método no BlockEntity para iniciar o cooldown global
                    // Passamos o 'player' para que o BE possa aplicar efeitos nele, se configurado
                    geradorBE.startGlobalCooldown(player);

                    // Mensagem para o jogador pode ser redundante se a tela já mostra, mas pode confirmar
                    // player.sendSystemMessage(Component.literal("§cFalha no reparo! Gerador em cooldown."), true);

                } else {
                    // O bloco já estava reparado quando a falha foi processada
                    LOGGER.warn("Jogador {} enviou FailResultPacket para Gerador em {}, mas ele não precisava de reparo.", player.getName().getString(), pos);
                }
            } else {
                // Bloco não encontrado ou tipo incorreto
                LOGGER.error("[CantinhoSpooky] Erro: FailResultPacket recebido para posição {} sem um GeradorBlockEntity.", pos);
            }
        });

        ctx.setPacketHandled(true); // Marca como processado
    }
}