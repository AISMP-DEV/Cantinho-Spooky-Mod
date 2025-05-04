package net.aqualoco.cantinhospooky.block.custom;

import net.aqualoco.cantinhospooky.network.GeradorPacket;
import net.aqualoco.cantinhospooky.network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkDirection;

public class GeradorBlock extends Block {
    public GeradorBlock(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            PacketHandler.CHANNEL.sendTo(
                    new GeradorPacket(),
                    ((ServerPlayer) player).connection.connection, // envia para o jogador
                    NetworkDirection.PLAY_TO_CLIENT
            );
        }
        return InteractionResult.SUCCESS;
    }
}
