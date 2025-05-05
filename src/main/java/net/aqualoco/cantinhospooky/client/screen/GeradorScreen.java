package net.aqualoco.cantinhospooky.client.screen;

// ----- Imports -----
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.aqualoco.cantinhospooky.Config; // Importar a classe Config
import net.aqualoco.cantinhospooky.network.CritSuccessPacket;
import net.aqualoco.cantinhospooky.network.FailResultPacket;
import net.aqualoco.cantinhospooky.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos; // Importado
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent; // Import SoundEvent
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;
// -------------------

public class GeradorScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();

    // --- Texturas ---
    private static final ResourceLocation MINIGAME_TEXTURE = new ResourceLocation("cantinhospooky", "textures/gui/gerador_completo.png");
    private static final ResourceLocation POINTER_TEXTURE = new ResourceLocation("cantinhospooky", "textures/gui/gerador_ponteiro.png");

    // --- Dimensões Texturas ---
    private static final int TEXTURE_WIDTH = 256; private static final int TEXTURE_HEIGHT = 256;
    private static final int POINTER_WIDTH = 12;  private static final int POINTER_HEIGHT = 8;

    // --- Configurações Visuais / Lógica Fixa ---
    private static final float POINTER_TRACK_RADIUS = 75f;
    private static final float CATCH_ZONE_WIDTH_DEGREES = 60f;
    private static final float CRIT_ZONE_WIDTH_DEGREES = 15f;
    private static final float ZONE_1_CENTER_ANGLE = 90f;
    private static final float ZONE_2_CENTER_ANGLE = 270f;
    // (Constantes ZONE_*_START/END calculadas uma vez)
    private static final float CATCH_ZONE_1_START = normalizeDegrees(ZONE_1_CENTER_ANGLE - CATCH_ZONE_WIDTH_DEGREES / 2f);
    private static final float CATCH_ZONE_1_END = normalizeDegrees(ZONE_1_CENTER_ANGLE + CATCH_ZONE_WIDTH_DEGREES / 2f);
    private static final float CRIT_ZONE_1_START = normalizeDegrees(ZONE_1_CENTER_ANGLE - CRIT_ZONE_WIDTH_DEGREES / 2f);
    private static final float CRIT_ZONE_1_END = normalizeDegrees(ZONE_1_CENTER_ANGLE + CRIT_ZONE_WIDTH_DEGREES / 2f);
    private static final float CATCH_ZONE_2_START = normalizeDegrees(ZONE_2_CENTER_ANGLE - CATCH_ZONE_WIDTH_DEGREES / 2f);
    private static final float CATCH_ZONE_2_END = normalizeDegrees(ZONE_2_CENTER_ANGLE + CATCH_ZONE_WIDTH_DEGREES / 2f);
    private static final float CRIT_ZONE_2_START = normalizeDegrees(ZONE_2_CENTER_ANGLE - CRIT_ZONE_WIDTH_DEGREES / 2f);
    private static final float CRIT_ZONE_2_END = normalizeDegrees(ZONE_2_CENTER_ANGLE + CRIT_ZONE_WIDTH_DEGREES / 2f);

    // --- Estado do Minigame ---
    private final BlockPos blockPos; // Posição do bloco sendo reparado
    private int currentPhase = 0;
    private float pointerDeg = 0f;
    private float currentPointerSpeed = 0f; // Definido por startPhase usando Config
    private int ticksRemainingThisPhase = 0; // Definido por startPhase usando Config
    private float randomRotationOffset = 0f;
    private boolean catchBonusUsedThisPhase = false;

    // Construtor atualizado
    public GeradorScreen(Component title, BlockPos pos) {
        super(title);
        this.blockPos = pos; // Armazena a posição recebida
    }

    @Override
    protected void init() {
        super.init();
        startPhase(1); // Inicia Fase 1
        playSound(SoundEvents.NOTE_BLOCK_PLING.get(), 1.0f, 1.0f); // Som inicial
    }

    // Método para configurar o início de uma fase
    private void startPhase(int phase) {
        if (phase > 3) {
            handleFinalSuccess(); // Já completou fase 3, é sucesso
            return;
        }
        LOGGER.info("Iniciando Fase {} para Gerador em {}", phase, this.blockPos);
        this.currentPhase = phase;
        this.catchBonusUsedThisPhase = false; // Reseta bônus a cada fase

        // Define velocidade e duração usando valores da Config via switch
        switch (phase) {
            case 1:
                this.currentPointerSpeed = (float) Config.phase1Speed; // Cast para float
                this.ticksRemainingThisPhase = Config.phase1DurationTicks;
                break;
            case 2:
                this.currentPointerSpeed = (float) Config.phase2Speed;
                this.ticksRemainingThisPhase = Config.phase2DurationTicks;
                break;
            case 3:
                this.currentPointerSpeed = (float) Config.phase3Speed;
                this.ticksRemainingThisPhase = Config.phase3DurationTicks;
                break;
            default: // Segurança: Se fase for inválida (ex: 0), encerra.
                LOGGER.error("Tentativa de iniciar fase inválida: {}", phase);
                closeScreen();
                return;
        }

        // Re-randomiza rotação da base e posição inicial do ponteiro
        RandomSource randomSource = getRandomSource();
        this.randomRotationOffset = randomSource.nextFloat() * 360.0f;
        this.pointerDeg = randomSource.nextFloat() * 360.0f;

        // Som opcional de início de fase
        // playSound(SoundEvents.NOTE_BLOCK_CHIME, 0.8f, 1.2f);
    }

    // Helper para obter RandomSource
    private RandomSource getRandomSource() {
        if (this.minecraft != null && this.minecraft.level != null) {
            return this.minecraft.level.random;
        }
        LOGGER.warn("Não foi possível obter RandomSource do level, usando Math.random().");
        return RandomSource.create((long) (Math.random() * Long.MAX_VALUE));
    }

    @Override public boolean isPauseScreen() { return false; }

    // --- RENDER ---
    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        // Não deve ter mudado significativamente, apenas usa this.currentPointerSpeed
        this.renderBackground(gg);
        int centerX = this.width / 2; int centerY = this.height / 2;
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        // Base Rotacionada
        gg.pose().pushPose();
        gg.pose().translate(centerX, centerY, 0);
        gg.pose().mulPose(Axis.ZP.rotationDegrees(this.randomRotationOffset));
        gg.blit(MINIGAME_TEXTURE, -TEXTURE_WIDTH / 2, -TEXTURE_HEIGHT / 2, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        gg.pose().popPose();

        // Ponteiro (com interpolação usando velocidade atual)
        float visualPointerAngle = Mth.rotLerp(partialTick, this.pointerDeg, this.pointerDeg + this.currentPointerSpeed);
        drawPointer(gg, centerX, centerY, visualPointerAngle);

        // Opcional: Desenhar Fase/Tempo
        // String phaseText = "Fase: " + this.currentPhase;
        // String timeText = String.format("Tempo: %.1f s", this.ticksRemainingThisPhase / 20.0f);
        // gg.drawString(this.font, phaseText, 10, 10, 0xFFFFFF);
        // gg.drawString(this.font, timeText, 10, 20, 0xFFFFFF);

        RenderSystem.disableBlend(); RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        super.render(gg, mouseX, mouseY, partialTick);
    }

    // drawPointer permanece o mesmo
    private void drawPointer(GuiGraphics gg, float centerX, float centerY, float visualAngle) {
        double rad = Math.toRadians(-visualAngle);
        float px = centerX + POINTER_TRACK_RADIUS * (float) Math.cos(rad);
        float py = centerY + POINTER_TRACK_RADIUS * (float) Math.sin(rad);
        gg.pose().pushPose();
        gg.pose().translate(px, py, 0);
        gg.pose().mulPose(Axis.ZP.rotationDegrees(-visualAngle + 90f));
        gg.blit(POINTER_TEXTURE, -POINTER_WIDTH / 2, -POINTER_HEIGHT / 2, 0, 0, POINTER_WIDTH, POINTER_HEIGHT, POINTER_WIDTH, POINTER_HEIGHT);
        gg.pose().popPose();
    }

    // --- tick() ---
    @Override
    public void tick() {
        super.tick();
        if (this.currentPhase <= 0) return; // Não faz nada se não iniciou ou já fechou

        // Atualiza posição do ponteiro com velocidade atual
        this.pointerDeg = normalizeDegrees(this.pointerDeg + this.currentPointerSpeed);

        // Verifica Timeout
        if (--this.ticksRemainingThisPhase <= 0) {
            handleFailure("Tempo esgotado na Fase " + this.currentPhase);
            return;
        }

        // Som do timer
        if (this.ticksRemainingThisPhase > 0 && this.ticksRemainingThisPhase % 20 == 0) {
            playSound(SoundEvents.NOTE_BLOCK_BIT.get(), 0.5f, 1.5f);
        }
    }

    // --- mouseClicked() ---
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Verifica se o jogo está ativo e foi clique esquerdo
        if (this.currentPhase > 0 && this.ticksRemainingThisPhase > 0 && button == 0) {
            float currentPointerAngle = normalizeDegrees(this.pointerDeg);
            float checkAngle = normalizeDegrees(-currentPointerAngle - this.randomRotationOffset); // Corrige pela rotação da base

            // Verifica Acerto Crítico (Amarelo)
            if (isInRange(checkAngle, CRIT_ZONE_1_START, CRIT_ZONE_1_END) ||
                    isInRange(checkAngle, CRIT_ZONE_2_START, CRIT_ZONE_2_END))
            {
                playSound(SoundEvents.NOTE_BLOCK_BELL.get(), 0.7f, 1.5f); // Som avanço/crítico
                if (this.currentPhase < 3) {
                    startPhase(this.currentPhase + 1); // Avança para próxima fase
                } else {
                    handleFinalSuccess(); // Completou fase 3!
                }
                return true; // Clique processado
            }
            // Verifica Acerto Normal (Verde)
            else if (isInRange(checkAngle, CATCH_ZONE_1_START, CATCH_ZONE_1_END) ||
                    isInRange(checkAngle, CATCH_ZONE_2_START, CATCH_ZONE_2_END))
            {
                if (!this.catchBonusUsedThisPhase) {
                    // Primeiro acerto verde: Bônus!
                    this.ticksRemainingThisPhase += Config.catchBonusTicks; // Usa valor da Config
                    this.catchBonusUsedThisPhase = true;
                    playSound(SoundEvents.NOTE_BLOCK_CHIME.get(), 0.6f, 1.2f);
                    // Usa chave de tradução
                    sendMessage(Component.translatable("message.cantinhospooky.gerador.bonus_time"));
                } else {
                    // Segundo acerto verde: Falha!
                    handleFailure("Acertou a área bônus duas vezes na Fase " + this.currentPhase);
                }
                return true; // Clique processado
            }
            // Errou as Zonas
            else
            {
                handleFailure("Errou as zonas de acerto na Fase " + this.currentPhase);
                return true; // Clique processado
            }
        }
        return super.mouseClicked(mouseX, mouseY, button); // Clique não foi processado aqui
    }


    // --- Handlers de Resultado Final ---
    private void handleFinalSuccess() {
        LOGGER.info("SUCESSO FINAL no reparo do Gerador em {}", this.blockPos);
        playSound(SoundEvents.PLAYER_LEVELUP, 0.8f, 1.0f);
        // Usa chave de tradução
        sendMessage(Component.translatable("message.cantinhospooky.gerador.success"));
        // Envia pacote com BlockPos
        PacketHandler.sendToServer(new CritSuccessPacket(this.blockPos));
        closeScreen();
    }

    private void handleFailure(String reason) {
        LOGGER.info("FALHA no reparo do Gerador em {}: {}", this.blockPos, reason);
        playSound(SoundEvents.VILLAGER_NO, 0.8f, 0.8f );
        // Usa chave de tradução
        sendMessage(Component.translatable("message.cantinhospooky.gerador.fail"));
        // Envia pacote com BlockPos
        PacketHandler.sendToServer(new FailResultPacket(this.blockPos));
        closeScreen();
    }

    // --- Métodos Auxiliares ---
    private static float normalizeDegrees(float degreesIn) { float r = degreesIn % 360.0F; return r >= 0.0F ? r : r + 360.0F; }
    private boolean isInRange(float angle, float start, float end) { float a = normalizeDegrees(angle); if (start <= end) { return a >= start && a <= end; } else { return a >= start || a <= end; } }

    private void playSound(SoundEvent sound, float volume, float pitch) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playNotifySound(sound, SoundSource.PLAYERS, volume, pitch);
        }
    }

    private void sendMessage(Component message) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(message, true); // true = na action bar
        }
    }

    private void closeScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
        // Zera a fase para parar o tick completamente
        this.currentPhase = 0;
        this.ticksRemainingThisPhase = 0;
    }
}