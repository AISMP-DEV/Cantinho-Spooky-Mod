package net.aqualoco.cantinhospooky.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.aqualoco.cantinhospooky.network.CritSuccessPacket;
import net.aqualoco.cantinhospooky.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;

// VERSÃO FINAL - COM ROTAÇÃO BASE - COM HITBOX ESPELHADA CORRIGIDA
public class GeradorScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();

    // --- Texturas ---
    private static final ResourceLocation MINIGAME_TEXTURE = new ResourceLocation("cantinhospooky", "textures/gui/gerador_completo.png");
    private static final ResourceLocation POINTER_TEXTURE = new ResourceLocation("cantinhospooky", "textures/gui/gerador_ponteiro.png");

    // --- Dimensões Texturas ---
    private static final int TEXTURE_WIDTH = 256; private static final int TEXTURE_HEIGHT = 256;
    private static final int POINTER_WIDTH = 12;  private static final int POINTER_HEIGHT = 8;

    // --- Configurações Minigame ---
    private float pointerDeg = 0f;
    private static final float POINTER_SPEED_PER_TICK = 6.0f; // Sua velocidade
    private int timeLeftTicks = 220; // Tempo inicial (11s)
    private float randomRotationOffset = 0f;
    private int catchHitCounter = 0;

    // --- Configurações Visuais / Lógica ---
    // << AJUSTE ESTES 3 VALORES APÓS TESTAR COM A VERSÃO DEBUG >>
    private static final float POINTER_TRACK_RADIUS = 75f;
    private static final float CATCH_ZONE_WIDTH_DEGREES = 60f;
    private static final float CRIT_ZONE_WIDTH_DEGREES = 15f;

    private static final float ZONE_1_CENTER_ANGLE = 90f;
    private static final float ZONE_2_CENTER_ANGLE = 270f;
    // (Constantes ZONE_*_START/END)
    private static final float CATCH_ZONE_1_START = normalizeDegrees(ZONE_1_CENTER_ANGLE - CATCH_ZONE_WIDTH_DEGREES / 2f);
    private static final float CATCH_ZONE_1_END = normalizeDegrees(ZONE_1_CENTER_ANGLE + CATCH_ZONE_WIDTH_DEGREES / 2f);
    private static final float CRIT_ZONE_1_START = normalizeDegrees(ZONE_1_CENTER_ANGLE - CRIT_ZONE_WIDTH_DEGREES / 2f);
    private static final float CRIT_ZONE_1_END = normalizeDegrees(ZONE_1_CENTER_ANGLE + CRIT_ZONE_WIDTH_DEGREES / 2f);
    private static final float CATCH_ZONE_2_START = normalizeDegrees(ZONE_2_CENTER_ANGLE - CATCH_ZONE_WIDTH_DEGREES / 2f);
    private static final float CATCH_ZONE_2_END = normalizeDegrees(ZONE_2_CENTER_ANGLE + CATCH_ZONE_WIDTH_DEGREES / 2f);
    private static final float CRIT_ZONE_2_START = normalizeDegrees(ZONE_2_CENTER_ANGLE - CRIT_ZONE_WIDTH_DEGREES / 2f);
    private static final float CRIT_ZONE_2_END = normalizeDegrees(ZONE_2_CENTER_ANGLE + CRIT_ZONE_WIDTH_DEGREES / 2f);

    public GeradorScreen(Component title) { super(title); }

    // --- init() ---
    @Override
    protected void init() {
        super.init();
        RandomSource randomSource = null;
        if (this.minecraft != null && this.minecraft.level != null) { randomSource = this.minecraft.level.random; }
        if (randomSource != null) {
            this.randomRotationOffset = randomSource.nextFloat() * 360.0f;
            this.pointerDeg = randomSource.nextFloat() * 360.0f;
        } else {
            LOGGER.warn("Não foi possível obter RandomSource do level, usando Math.random().");
            this.randomRotationOffset = (float) (Math.random() * 360.0);
            this.pointerDeg = (float) (Math.random() * 360.0);
        }
        LOGGER.info("GeradorScreen inicializado com rotação offset: {}", String.format("%.1f", this.randomRotationOffset));
        LOGGER.info("Posição inicial do ponteiro: {}", String.format("%.1f", this.pointerDeg));
        this.timeLeftTicks = 220; // Define seu tempo inicial
        this.catchHitCounter = 0;
        Minecraft mc = Minecraft.getInstance();
        // Som de início (Corrigido)
        if (mc.player != null && mc.level != null) { mc.level.playSound(mc.player, mc.player.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.PLAYERS, 1.0f, 1.0f ); }
    }

    @Override public boolean isPauseScreen() { return false; }

    // --- RENDER ---
    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
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
        // Ponteiro (com interpolação Mth.rotLerp)
        float visualPointerAngle = Mth.rotLerp(partialTick, this.pointerDeg, this.pointerDeg + POINTER_SPEED_PER_TICK);
        drawPointer(gg, centerX, centerY, visualPointerAngle);
        RenderSystem.disableBlend(); RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        super.render(gg, mouseX, mouseY, partialTick);
    }

    /** Desenha apenas o ponteiro (Sua versão visualmente correta) */
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
        this.pointerDeg += POINTER_SPEED_PER_TICK;

        if (--timeLeftTicks <= 0) {
            handleFailure(true); // Chama falha no timeout
            return;
        }
        // Som do timer (Corrigido)
        if (this.timeLeftTicks > 0 && this.timeLeftTicks % 20 == 0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) { mc.level.playSound(mc.player, mc.player.blockPosition(), SoundEvents.NOTE_BLOCK_BIT.get(), SoundSource.PLAYERS, 0.5f, 1.5f); }
        }
    }

    // --- mouseClicked() COM CORREÇÃO DE ESPELHAMENTO ---
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Só permite clique se o tempo não acabou (na versão final)
        if (timeLeftTicks > 0 && button == 0) {
            float currentPointerAngle = normalizeDegrees(this.pointerDeg);

            // <<< CORREÇÃO HITBOX: Espelha E Ajusta pelo Offset >>>
            float checkAngle = normalizeDegrees(-currentPointerAngle - this.randomRotationOffset);

            // 1. Verifica Acerto Crítico
            if (isInRange(checkAngle, CRIT_ZONE_1_START, CRIT_ZONE_1_END) ||
                    isInRange(checkAngle, CRIT_ZONE_2_START, CRIT_ZONE_2_END))
            {
                handleCritHit(); // Chama handler que fecha a tela
                return true;
            }
            // 2. Verifica Acerto Normal
            else if (isInRange(checkAngle, CATCH_ZONE_1_START, CATCH_ZONE_1_END) ||
                    isInRange(checkAngle, CATCH_ZONE_2_START, CATCH_ZONE_2_END))
            {
                this.catchHitCounter++;
                if (this.catchHitCounter <= 2) {
                    handleCatchHit(); // Chama handler que adiciona tempo
                } else {
                    handleFailure(false); // Chama handler que fecha a tela
                }
                return true;
            }
            // 3. Errou
            else
            {
                handleFailure(false); // Chama handler que fecha a tela
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // --- HANDLERS (VERSÃO NORMAL QUE FECHA A TELA) ---

    private void handleCritHit() {
        LOGGER.info("Acerto CRÍTICO!");
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            // Som (Corrigido)
            mc.level.playSound(mc.player, mc.player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.0f);
            mc.player.sendSystemMessage(Component.literal("Acerto crítico!"));
        }
        PacketHandler.sendToServer(new CritSuccessPacket());
        this.minecraft.setScreen(null); // Fecha
    }

    private void handleCatchHit() {
        LOGGER.info("Acerto normal #{} - Ganhando tempo extra.", this.catchHitCounter);
        Minecraft mc = Minecraft.getInstance();
        this.timeLeftTicks += 80; // Adiciona tempo
        if (mc.player != null && mc.level != null) {
            // Som (Corrigido)
            mc.level.playSound(mc.player, mc.player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.get(), SoundSource.PLAYERS, 0.6f, 1.2f);
            mc.player.sendSystemMessage(Component.literal("Tempo extra! Tente o acerto crítico!"));
        }
        // Não fecha
    }

    private void handleFailure(boolean isTimeout) {
        if(isTimeout) { LOGGER.info("Falha por Tempo Esgotado!"); }
        else { LOGGER.info("Falha por Erro ou Tentativas Excedidas!"); }
        Minecraft mc = Minecraft.getInstance();
        // Toca som de falha (CORRIGIDO para tocar sempre)
        if (mc.player != null && mc.level != null) {
            mc.level.playSound(mc.player, mc.player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 0.8f );
        }
        // PacketHandler.sendToServer(new FailResultPacket()); // Opcional
        if (mc.player != null) { mc.player.sendSystemMessage(Component.literal("Falha no minigame!")); }
        this.minecraft.setScreen(null); // Fecha
    }


    // --- Métodos auxiliares ---
    private static float normalizeDegrees(float degreesIn) { float r = degreesIn % 360.0F; return r >= 0.0F ? r : r + 360.0F; }
    private boolean isInRange(float angle, float start, float end) { float a = normalizeDegrees(angle); if (start <= end) { return a >= start && a <= end; } else { return a >= start || a <= end; } }

} // Fim da classe GeradorScreen