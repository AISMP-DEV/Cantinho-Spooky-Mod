package net.aqualoco.cantinhospooky;

// Imports necessários
import com.mojang.logging.LogUtils; // Ou use o LogManager
import net.aqualoco.cantinhospooky.client.screen.GeradorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos; // <-- Importar BlockPos
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
// import com.mojang.logging.LogManager; // Se usar LogManager

// Garanta que CommonProxy está no pacote correto se não for no mesmo
// import net.aqualoco.cantinhospooky.CommonProxy;

public class ClientProxy extends CommonProxy {
    // Usar o logger do seu mod principal ou o LogManager
    private static final Logger LOGGER = CantinhoSpooky.LOGGER; // Ou LogUtils.getLogger();

    // ---> ALTERAÇÃO AQUI <---
    // 1. Modificar a assinatura para aceitar BlockPos
    public static void handleOpenGeradorScreen(BlockPos pos) {
        // Log pode incluir a posição agora
        LOGGER.debug("[Gerador] handleOpenGeradorScreen() chamado para {}", pos);

        // 2. Passar a 'pos' para o construtor da GeradorScreen
        //    (Isso vai dar erro até modificarmos GeradorScreen para aceitar a pos)
        Minecraft.getInstance().setScreen(
                new GeradorScreen(Component.literal("Reparar Gerador"), pos) // <-- Passa a pos
        );
    }

    // O método setup() do CommonProxy (que chama PacketHandler.register())
    // não precisa ser sobrescrito aqui, a menos que você tenha setup
    // específico do cliente que precise rodar durante FMLClientSetupEvent.
    // @Override
    // public void setup(FMLClientSetupEvent event) {
    //    super.setup(event); // Chama o setup comum (PacketHandler.register)
    //    // Registre aqui MenuScreens, BlockEntityRenderers, Keybindings, etc.
    // }
}
