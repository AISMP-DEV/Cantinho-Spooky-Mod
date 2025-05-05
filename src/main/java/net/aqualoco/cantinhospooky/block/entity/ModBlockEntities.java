package net.aqualoco.cantinhospooky.block.entity;

import net.aqualoco.cantinhospooky.CantinhoSpooky;
import net.aqualoco.cantinhospooky.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    // 1. Cria o DeferredRegister para BlockEntityTypes
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CantinhoSpooky.MOD_ID);

    // 2. Registra o BlockEntityType para o Gerador
    //    - O nome ("gerador") deve ser o mesmo usado para registrar o bloco.
    //    - '.Builder.of(GeradorBlockEntity::new, ModBlocks.GERADOR.get())' cria o tipo:
    //        - GeradorBlockEntity::new : Fornece o construtor do nosso BlockEntity.
    //        - ModBlocks.GERADOR.get() : Especifica a qual bloco este BlockEntity pertence.
    //    - '.build(null)' finaliza a construção (o argumento é para DataFixerUpper, geralmente null para mods simples).
    public static final RegistryObject<BlockEntityType<GeradorBlockEntity>> GERADOR_BE =
            BLOCK_ENTITIES.register("gerador", () ->
                    BlockEntityType.Builder.of(GeradorBlockEntity::new, ModBlocks.GERADOR.get())
                            .build(null));


    // 3. Método para registrar o DeferredRegister no barramento de eventos do mod
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}