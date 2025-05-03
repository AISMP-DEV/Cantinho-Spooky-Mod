package net.aqualoco.cantinhospooky.item;

import net.aqualoco.cantinhospooky.CantinhoSpooky;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Objects;

// Esse cara vai definir todos os itens que eu adicionar no meu modzinho bonitinho.
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CantinhoSpooky.MOD_ID);

    public static final RegistryObject<Item> GERADOR = ITEMS.register("gerador",
            () -> new Item(new Item.Properties()));




    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
