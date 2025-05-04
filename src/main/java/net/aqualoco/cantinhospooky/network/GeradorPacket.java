package net.aqualoco.cantinhospooky.network;

import net.minecraft.network.FriendlyByteBuf;

public record GeradorPacket() {
    // Encode (não há dados a enviar por enquanto)
    public static void encode(GeradorPacket msg, FriendlyByteBuf buf) {}

    // Decode
    public static GeradorPacket decode(FriendlyByteBuf buf) {
        return new GeradorPacket();
    }
}

