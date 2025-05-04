package net.aqualoco.cantinhospooky.network;

import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

    public class CritSuccessPacket {

        private static final Logger LOGGER = LogUtils.getLogger();

        public CritSuccessPacket() {
        }

        public static void encode(CritSuccessPacket msg, FriendlyByteBuf buf) {
        }

        public static CritSuccessPacket decode(FriendlyByteBuf buf) {
            return new CritSuccessPacket();
        }

        public static void handle(CritSuccessPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            if (ctx.getDirection().getReceptionSide() != LogicalSide.SERVER) {
                LOGGER.error("[CantinhoSpooky] Erro: pacote CritSuccess recebido no lado errado.");
                ctx.setPacketHandled(true);
                return;
            }

            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player != null && player.level() != null) {
                    ServerLevel level = (ServerLevel) player.level();
                    level.playSound(null, player.blockPosition(),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS,
                            1.0f, 1.5f);
                    player.sendSystemMessage(Component.literal("§6Acerto crítico!"));
                } else {
                    LOGGER.error("[CantinhoSpooky] Erro: player ou level nulo ao tentar tocar som.");
                }
            });

            ctx.setPacketHandled(true);
        }
    }