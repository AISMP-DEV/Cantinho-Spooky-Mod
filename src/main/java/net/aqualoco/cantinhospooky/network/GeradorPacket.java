package net.aqualoco.cantinhospooky.network;

import net.minecraft.core.BlockPos; // <-- Importar BlockPos
import net.minecraft.network.FriendlyByteBuf;

// 1. Adicionar o campo 'BlockPos pos' à definição do record
public record GeradorPacket(BlockPos pos) {

    // 2. Encode: Escrever a BlockPos no buffer
    public static void encode(GeradorPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos()); // Usa o método padrão para escrever BlockPos
    }

    // 3. Decode: Ler a BlockPos do buffer e criar o record com ela
    public static GeradorPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos(); // Usa o método padrão para ler BlockPos
        return new GeradorPacket(pos);
    }
}