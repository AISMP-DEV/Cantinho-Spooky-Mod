package net.aqualoco.cantinhospooky;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair; // Import necessário para SPEC

// Mantém a anotação para registrar o listener do evento de config
@Mod.EventBusSubscriber(modid = CantinhoSpooky.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    // Classe interna para configurações COMUNS (server e client)
    // Poderíamos ter COMMON, CLIENT, SERVER separadamente se necessário
    public static class Common {
        // Constantes para limites (opcional, mas bom para clareza)
        private static final int MIN_TICKS = 1; // 1 tick
        private static final int MAX_TICKS = 20 * 60 * 10; // 10 minutos em ticks
        private static final double MIN_SPEED = 0.1;
        private static final double MAX_SPEED = 50.0;

        // --- Configurações do Gerador ---
        public final ForgeConfigSpec.IntValue globalCooldownTicks;

        // --- Configurações do QTE ---
        public final ForgeConfigSpec.IntValue phase1DurationTicks;
        public final ForgeConfigSpec.IntValue phase2DurationTicks;
        public final ForgeConfigSpec.IntValue phase3DurationTicks;
        public final ForgeConfigSpec.DoubleValue phase1Speed; // Usando Double para velocidade
        public final ForgeConfigSpec.DoubleValue phase2Speed;
        public final ForgeConfigSpec.DoubleValue phase3Speed;
        public final ForgeConfigSpec.IntValue catchBonusTicks;


        // Construtor onde definimos as configurações
        Common(ForgeConfigSpec.Builder builder) {
            builder.push("gerador_settings"); // Agrupa no arquivo TOML

            globalCooldownTicks = builder
                    .comment("Duration of the global cooldown after a failed repair attempt, in ticks (20 ticks = 1 second).")
                    .translation("config.cantinhospooky.globalCooldownTicks") // Chave para tradução no lang
                    .defineInRange("globalCooldownTicks", 20 * 20, MIN_TICKS, MAX_TICKS); // Default 20s

            builder.pop(); // Fecha o grupo gerador_settings

            builder.push("qte_settings"); // Novo grupo para QTE

            phase1DurationTicks = builder
                    .comment("Duration of QTE Phase 1, in ticks.")
                    .translation("config.cantinhospooky.phase1DurationTicks")
                    .defineInRange("phase1DurationTicks", 20 * 10, MIN_TICKS, MAX_TICKS); // Default 10s

            phase2DurationTicks = builder
                    .comment("Duration of QTE Phase 2, in ticks.")
                    .translation("config.cantinhospooky.phase2DurationTicks")
                    .defineInRange("phase2DurationTicks", 20 * 8, MIN_TICKS, MAX_TICKS); // Default 8s

            phase3DurationTicks = builder
                    .comment("Duration of QTE Phase 3, in ticks.")
                    .translation("config.cantinhospooky.phase3DurationTicks")
                    .defineInRange("phase3DurationTicks", 20 * 6, MIN_TICKS, MAX_TICKS); // Default 6s

            phase1Speed = builder
                    .comment("Pointer speed during QTE Phase 1 (degrees per tick).")
                    .translation("config.cantinhospooky.phase1Speed")
                    .defineInRange("phase1Speed", 6.0, MIN_SPEED, MAX_SPEED); // Default 6.0

            phase2Speed = builder
                    .comment("Pointer speed during QTE Phase 2.")
                    .translation("config.cantinhospooky.phase2Speed")
                    .defineInRange("phase2Speed", 7.0, MIN_SPEED, MAX_SPEED); // Default 7.0

            phase3Speed = builder
                    .comment("Pointer speed during QTE Phase 3.")
                    .translation("config.cantinhospooky.phase3Speed")
                    .defineInRange("phase3Speed", 8.0, MIN_SPEED, MAX_SPEED); // Default 8.0

            catchBonusTicks = builder
                    .comment("Time bonus in ticks when hitting the catch zone (green).")
                    .translation("config.cantinhospooky.catchBonusTicks")
                    .defineInRange("catchBonusTicks", 20 * 2, 0, MAX_TICKS); // Default 2s, min 0

            builder.pop(); // Fecha o grupo qte_settings
        }
    }

    // --- Instâncias e SPEC ---
    // Expõe a configuração COMUM
    public static final Common COMMON;
    // Expõe o SPEC para registro
    public static final ForgeConfigSpec COMMON_SPEC;

    // Bloco estático para inicializar as instâncias (Forge recomenda este padrão)
    static {
        // Cria um par (Configuração, Spec)
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        // Separa o Spec e a Configuração
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    // --- Campos Estáticos para Acesso Fácil (Atualizados no onLoad) ---
    // Estes campos guardarão os valores carregados para não precisar chamar .get() toda hora
    public static int globalCooldownTicks;
    public static int phase1DurationTicks;
    public static int phase2DurationTicks;
    public static int phase3DurationTicks;
    public static double phase1Speed; // Usando double aqui também
    public static double phase2Speed;
    public static double phase3Speed;
    public static int catchBonusTicks;

    // Método que carrega os valores da config para os campos estáticos quando a config é carregada/recarregada
    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading event) {
        // Verifica se a config sendo carregada é a nossa COMMON_SPEC
        if (event.getConfig().getSpec() == Config.COMMON_SPEC) {
            loadCommonConfig();
            CantinhoSpooky.LOGGER.info("CantinhoSpooky Common Config loaded.");
        }
    }
    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == Config.COMMON_SPEC) {
            loadCommonConfig();
            CantinhoSpooky.LOGGER.info("CantinhoSpooky Common Config reloaded.");
        }
    }

    private static void loadCommonConfig() {
        globalCooldownTicks = COMMON.globalCooldownTicks.get();
        phase1DurationTicks = COMMON.phase1DurationTicks.get();
        phase2DurationTicks = COMMON.phase2DurationTicks.get();
        phase3DurationTicks = COMMON.phase3DurationTicks.get();
        phase1Speed = COMMON.phase1Speed.get(); // Obtém como Double
        phase2Speed = COMMON.phase2Speed.get();
        phase3Speed = COMMON.phase3Speed.get();
        catchBonusTicks = COMMON.catchBonusTicks.get();
    }

    // Adicionar as chaves de tradução usadas acima nos arquivos lang/en_us.json e lang/pt_br.json
    // Exemplo para en_us.json:
    // "config.cantinhospooky.globalCooldownTicks": "Global Cooldown Ticks",
    // "config.cantinhospooky.phase1DurationTicks": "Phase 1 Duration Ticks",
    // ...etc
}