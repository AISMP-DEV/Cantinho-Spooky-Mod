package net.aqualoco.cantinhospooky.network;

import net.aqualoco.cantinhospooky.CantinhoSpooky; // Para o LOGGER
import net.aqualoco.cantinhospooky.block.entity.GeradorBlockEntity; // <-- Importar o BE
import net.minecraft.core.BlockPos;             // <-- Importar BlockPos
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity; // <-- Importar BlockEntity base
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor; // Pode ser útil para sons/partículas futuras
import org.slf4j.Logger;
import com.mojang.logging.LogUtils; // Importar LogUtils se não estiver lá

import java.util.function.Supplier;

public class CritSuccessPacket {

    private static final Logger LOGGER = LogUtils.getLogger(); // Garanta que está usando o logger

    // 1. Adicionar o campo BlockPos
    private final BlockPos pos;

    // 2. Modificar o construtor para aceitar BlockPos
    public CritSuccessPacket(BlockPos pos) {
        this.pos = pos;
    }

    // 3. Modificar encode para escrever a BlockPos
    public static void encode(CritSuccessPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    // 4. Modificar decode para ler a BlockPos e criar o pacote
    public static CritSuccessPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new CritSuccessPacket(pos);
    }

    // 5. Modificar o handle para USAR a BlockPos e interagir com o BlockEntity
    public static void handle(CritSuccessPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        // A verificação de lado é feita implicitamente pela forma como registramos C->S no PacketHandler,
        // mas podemos manter uma verificação pelo 'sender' não ser nulo.
        ServerPlayer player = ctx.getSender();
        if (player == null) {
            LOGGER.error("[CantinhoSpooky] Erro: CritSuccessPacket recebido sem remetente (não no servidor?)");
            ctx.setPacketHandled(true);
            return;
        }

        ctx.enqueueWork(() -> { // Continua essencial rodar na thread principal do servidor
            ServerLevel level = player.serverLevel(); // Pega o nível do jogador

            // --- Interação com o BlockEntity ---
            BlockEntity blockEntity = level.getBlockEntity(msg.pos); // Usa a POSIÇÃO do pacote

            if (blockEntity instanceof GeradorBlockEntity geradorBE) {
                // Verifica se o bloco AINDA precisa de reparo (evita processar pacote duas vezes ou trapaça simples)
                if (geradorBE.needsRepair()) { // Acessamos um campo interno, idealmente teríamos um getter getNeedsRepair() no BE
                    LOGGER.info("Jogador {} reparou Gerador em {}", player.getName().getString(), msg.pos);

                    // Chama o método no BlockEntity para marcar como reparado!
                    geradorBE.setRepaired();

                    // Feedback (som e mensagem) continua útil
                    level.playSound(null, msg.pos, // Toca o som na posição do bloco
                            SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, // Som de sucesso, categoria BLOCKS
                            1.0f, 1.2f);
                    player.sendSystemMessage(Component.translatable("message.cantinhospooky.gerador.success"));

                } else {
                    // O bloco já foi reparado (talvez por outro jogador ou pacote duplicado)
                    LOGGER.warn("Jogador {} enviou CritSuccessPacket para Gerador em {}, mas já estava reparado.", player.getName().getString(), msg.pos);
                    player.sendSystemMessage(Component.translatable("message.cantinhospooky.gerador.repaired"), true); // Informa o jogador
                }

            } else {
                // O bloco na posição não é o nosso BlockEntity (ou não existe mais)
                LOGGER.error("[CantinhoSpooky] Erro: CritSuccessPacket recebido para posição {} que não contém um GeradorBlockEntity.", msg.pos);
            }
            // --- Fim da Interação com o BlockEntity ---
        });

        ctx.setPacketHandled(true); // Marca o pacote como processado
    }
}