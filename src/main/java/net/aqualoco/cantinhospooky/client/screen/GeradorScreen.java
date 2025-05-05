package net.aqualoco.cantinhospooky.client.screen;

// ----- Imports -----
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.aqualoco.cantinhospooky.Config; // Importar a classe Config
import net.aqualoco.cantinhospooky.network.CritSuccessPacket;
import net.aqualoco.cantinhospooky.network.FailResultPacket;
import net.aqualoco.cantinhospooky.network.PacketHandler;
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
    private static final ResourceLocation POINTER_TEXTURE = new ResourceLocation("cantinhospooky", "textures/gui/gerador_ponteiro.png"); // Será sua chave inglesa

    // --- Dimensões Texturas ---
    private static final int TEXTURE_WIDTH = 256; private static final int TEXTURE_HEIGHT = 256;
    private static final int POINTER_WIDTH = 12;  private static final int POINTER_HEIGHT = 8; // Ajuste se a chave inglesa tiver tamanho diferente

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
    private float pointerDeg = 0f; // Posição angular atual
    private float currentPointerSpeed = 0f; // Velocidade angular atual (vem da Config)
    private int ticksRemainingThisPhase = 0; // Tempo restante na fase atual (vem da Config)
    private float randomRotationOffset = 0f; // Rotação da imagem de fundo
    private boolean catchBonusUsedThisPhase = false; // Se o bônus de tempo já foi usado nesta fase
    private float pointerDirection = 1.0f; // Direção do ponteiro (1.0f ou -1.0f)

    /** Construtor que recebe o título e a posição do bloco */
    public GeradorScreen(Component title, BlockPos pos) {
        super(title);
        this.blockPos = pos; // Armazena a posição
    }

    /** Chamado quando a tela é aberta */
    @Override
    protected void init() {
        super.init();
        // Define estado inicial antes de começar a primeira fase
        RandomSource randomSource = getRandomSource();
        this.pointerDirection = randomSource.nextBoolean() ? -1.0f : 1.0f; // Direção inicial aleatória
        this.pointerDeg = randomSource.nextFloat() * 360.0f; // Posição inicial aleatória
        startPhase(1); // Configura e inicia a Fase 1
        playSound(SoundEvents.NOTE_BLOCK_PLING.get(), 1.0f, 1.0f); // Som de início geral
    }

    /** Configura o estado interno para iniciar uma fase específica */
    private void startPhase(int phase) {
        if (phase > 3) {
            handleFinalSuccess(); // Se tentar ir além da 3, considera sucesso final
            return;
        }
        // LOGGER.info("Iniciando Fase {} para Gerador em {}", phase, this.blockPos);
        this.currentPhase = phase;
        this.catchBonusUsedThisPhase = false; // Reseta o uso do bônus

        // Define velocidade e duração buscando os valores da classe Config
        switch (phase) {
            case 1:
                this.currentPointerSpeed = (float) Config.phase1Speed; // Usa valor da Config
                this.ticksRemainingThisPhase = Config.phase1DurationTicks; // Usa valor da Config
                break;
            case 2:
                this.currentPointerSpeed = (float) Config.phase2Speed;
                this.ticksRemainingThisPhase = Config.phase2DurationTicks;
                break;
            case 3:
                this.currentPointerSpeed = (float) Config.phase3Speed;
                this.ticksRemainingThisPhase = Config.phase3DurationTicks;
                break;
            default:
                LOGGER.error("Tentativa de iniciar fase inválida: {}", phase);
                closeScreen(); // Fecha a tela se a fase for inválida
                return;
        }

        // Re-randomiza a rotação da base e a direção do ponteiro
        RandomSource randomSource = getRandomSource();
        this.randomRotationOffset = randomSource.nextFloat() * 360.0f; // Rotação da textura de fundo
        // A posição angular (this.pointerDeg) NÃO é resetada

        // Lógica para chance de 66% de MUDAR a direção atual
        float changeDirectionChance = randomSource.nextFloat(); // Gera um número entre 0.0 e 1.0
        if (changeDirectionChance < 0.66f) { // Aproximadamente 66% de chance
            this.pointerDirection *= -1.0f; // Inverte a direção atual
            LOGGER.info("Iniciando Fase {} (Gerador {}): Direção INVERTIDA para {}", phase, this.blockPos, this.pointerDirection > 0 ? "Horário" : "Anti-Horário");
        } else {
            // Mantém a direção atual (não precisa fazer nada no valor, só loga)
            LOGGER.info("Iniciando Fase {} (Gerador {}): Direção MANTIDA como {}", phase, this.blockPos, this.pointerDirection > 0 ? "Horário" : "Anti-Horário");
        }
        // Som opcional de início de fase
    }

    /** Helper para obter uma fonte aleatória segura */
    private RandomSource getRandomSource() {
        if (this.minecraft != null && this.minecraft.level != null) {
            return this.minecraft.level.random;
        }
        LOGGER.warn("Não foi possível obter RandomSource do level, usando Math.random().");
        return RandomSource.create((long) (Math.random() * Long.MAX_VALUE));
    }

    /** Informa se esta tela pausa o jogo (não pausa) */
    @Override public boolean isPauseScreen() { return false; }

    // --- RENDER ---
    /** Desenha a tela a cada frame */
    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        // Desenha o fundo escurecido padrão
        this.renderBackground(gg);
        int centerX = this.width / 2; int centerY = this.height / 2;

        // Configurações de renderização para texturas com transparência
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f); // Cor branca, sem transparência extra
        RenderSystem.setShader(GameRenderer::getPositionTexShader); // Shader padrão para GUI com textura

        // --- Base Rotacionada ---
        gg.pose().pushPose(); // Salva estado atual da matriz de transformação
        gg.pose().translate(centerX, centerY, 0); // Move a origem para o centro da tela
        gg.pose().mulPose(Axis.ZP.rotationDegrees(this.randomRotationOffset)); // Aplica a rotação aleatória da base
        // Desenha a textura da base centralizada na nova origem
        gg.blit(MINIGAME_TEXTURE, -TEXTURE_WIDTH / 2, -TEXTURE_HEIGHT / 2, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        gg.pose().popPose(); // Restaura o estado anterior da matriz

        // --- Ponteiro (Chave Inglesa) ---
        // Calcula o ângulo visual interpolado entre ticks, considerando a direção
        float nextAngle = this.pointerDeg + this.currentPointerSpeed * this.pointerDirection;
        float visualPointerAngle = Mth.rotLerp(partialTick, this.pointerDeg, nextAngle);
        // Chama o método auxiliar para desenhar o ponteiro
        drawPointer(gg, centerX, centerY, visualPointerAngle);

        // --- Informações de Debug/UI (Opcional) ---
        // Descomente e ajuste se quiser mostrar informações na tela
        // String phaseText = "Fase: " + this.currentPhase;
        // String timeText = String.format("Tempo: %.1f s", this.ticksRemainingThisPhase / 20.0f);
        // gg.drawString(this.font, phaseText, 10, 10, 0xFFFFFF); // Cor branca
        // gg.drawString(this.font, timeText, 10, 20, 0xFFFFFF);

        // Reseta configurações de renderização
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        // Chama o render da superclasse (geralmente não faz nada para Screen base)
        super.render(gg, mouseX, mouseY, partialTick);
    }

    /** Desenha apenas o ponteiro (textura da chave inglesa) na posição correta, sem rotacioná-lo */
    private void drawPointer(GuiGraphics gg, float centerX, float centerY, float visualAngle) {
        // Calcula a posição (x, y) ao longo do círculo
        double rad = Math.toRadians(-visualAngle); // Converte ângulo para radianos (negativo pode depender do sistema de coordenadas desejado)
        float px = centerX + POINTER_TRACK_RADIUS * (float) Math.cos(rad);
        float py = centerY + POINTER_TRACK_RADIUS * (float) Math.sin(rad);

        gg.pose().pushPose(); // Salva matriz
        gg.pose().translate(px, py, 0); // Move para a posição (px, py)

        // A linha que rotacionava a textura
        gg.pose().mulPose(Axis.ZP.rotationDegrees(-visualAngle + 90f));

        // Desenha a textura do ponteiro (chave inglesa) centralizada em (px, py)
        gg.blit(POINTER_TEXTURE, -POINTER_WIDTH / 2, -POINTER_HEIGHT / 2, 0, 0, POINTER_WIDTH, POINTER_HEIGHT, POINTER_WIDTH, POINTER_HEIGHT);
        gg.pose().popPose(); // Restaura matriz
    }

    // --- tick() ---
    /** Chamado a cada tick do jogo (20 vezes por segundo) */
    @Override
    public void tick() {
        super.tick(); // Chama tick da superclasse

        // Se o minigame não está ativo (fase 0), não faz nada
        if (this.currentPhase <= 0) return;

        // Atualiza a posição angular do ponteiro usando velocidade e direção atuais
        this.pointerDeg = normalizeDegrees(this.pointerDeg + this.currentPointerSpeed * this.pointerDirection);

        // Decrementa o tempo restante e verifica se acabou
        if (--this.ticksRemainingThisPhase <= 0) {
            handleFailure("Tempo esgotado na Fase " + this.currentPhase);
            return; // Importante retornar para não executar mais nada no tick após fechar
        }

        // Toca som de "tick" a cada segundo
        if (this.ticksRemainingThisPhase > 0 && this.ticksRemainingThisPhase % 20 == 0) {
            playSound(SoundEvents.NOTE_BLOCK_BIT.get(), 0.5f, 1.5f);
        }
    }

    // --- mouseClicked() ---
    /** Chamado quando o mouse é clicado */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Processa apenas clique esquerdo (button 0) enquanto o minigame está ativo
        if (this.currentPhase > 0 && this.ticksRemainingThisPhase > 0 && button == 0) {
            // Pega o ângulo lógico atual do ponteiro
            float currentPointerAngle = normalizeDegrees(this.pointerDeg);
            // Calcula o ângulo de checagem, ajustando pela rotação da base
            float checkAngle = normalizeDegrees(-currentPointerAngle - this.randomRotationOffset);

            // --- Verifica Acerto Crítico (Amarelo) ---
            if (isInRange(checkAngle, CRIT_ZONE_1_START, CRIT_ZONE_1_END) ||
                    isInRange(checkAngle, CRIT_ZONE_2_START, CRIT_ZONE_2_END))
            {
                playSound(SoundEvents.NOTE_BLOCK_BELL.get(), 0.7f, 1.5f); // Som de acerto crítico/avanço
                if (this.currentPhase < 3) {
                    startPhase(this.currentPhase + 1); // Avança para a próxima fase
                } else {
                    handleFinalSuccess(); // Acertou o crítico na última fase: Vitória!
                }
                return true; // Indica que o clique foi processado
            }
            // --- Verifica Acerto Normal (Verde) ---
            else if (isInRange(checkAngle, CATCH_ZONE_1_START, CATCH_ZONE_1_END) ||
                    isInRange(checkAngle, CATCH_ZONE_2_START, CATCH_ZONE_2_END))
            {
                if (!this.catchBonusUsedThisPhase) {
                    // Primeiro acerto na zona verde nesta fase: Ganha bônus de tempo
                    this.ticksRemainingThisPhase += Config.catchBonusTicks; // Usa valor da Config
                    this.catchBonusUsedThisPhase = true; // Marca que já usou o bônus nesta fase
                    playSound(SoundEvents.NOTE_BLOCK_CHIME.get(), 0.6f, 1.2f); // Som de bônus
                    sendMessage(Component.translatable("message.cantinhospooky.gerador.bonus_time")); // Mensagem de bônus
                } else {
                    // Segundo acerto na zona verde nesta fase: Falha!
                    handleFailure("Acertou a área bônus duas vezes na Fase " + this.currentPhase);
                }
                return true; // Indica que o clique foi processado
            }
            // --- Errou as Zonas ---
            else
            {
                // Clicou fora das zonas amarela e verde: Falha!
                handleFailure("Errou as zonas de acerto na Fase " + this.currentPhase);
                return true; // Indica que o clique foi processado
            }
        }
        // Se não foi clique esquerdo ou o minigame não estava ativo, deixa a superclasse lidar
        return super.mouseClicked(mouseX, mouseY, button);
    }


    // --- Handlers de Resultado Final ---

    /** Chamado ao completar com sucesso a Fase 3 */
    private void handleFinalSuccess() {
        LOGGER.info("SUCESSO FINAL no reparo do Gerador em {}", this.blockPos);
        playSound(SoundEvents.PLAYER_LEVELUP, 0.8f, 1.0f); // Som de vitória
        sendMessage(Component.translatable("message.cantinhospooky.gerador.success")); // Mensagem de vitória (traduzida)
        PacketHandler.sendToServer(new CritSuccessPacket(this.blockPos)); // Envia pacote de sucesso com a posição
        closeScreen(); // Fecha a tela
    }

    /** Chamado em qualquer condição de falha (timeout, erro, segundo acerto verde) */
    private void handleFailure(String reason) {
        LOGGER.info("FALHA no reparo do Gerador em {}: {}", this.blockPos, reason); // Loga a razão da falha
        playSound(SoundEvents.VILLAGER_NO, 0.8f, 0.8f ); // Som de falha
        sendMessage(Component.translatable("message.cantinhospooky.gerador.fail")); // Mensagem de falha (traduzida)
        PacketHandler.sendToServer(new FailResultPacket(this.blockPos)); // Envia pacote de falha com a posição
        closeScreen(); // Fecha a tela
    }

    // --- Métodos Auxiliares ---

    /** Normaliza um ângulo para o intervalo [0, 360) */
    private static float normalizeDegrees(float degreesIn) {
        float r = degreesIn % 360.0F;
        return r >= 0.0F ? r : r + 360.0F;
    }

    /** Verifica se um ângulo está dentro de um intervalo (lida com wrap around 360->0) */
    private boolean isInRange(float angle, float start, float end) {
        float a = normalizeDegrees(angle); // Garante que o ângulo a ser testado está normalizado
        if (start <= end) {
            // Intervalo normal (ex: 10 a 50)
            return a >= start && a <= end;
        } else {
            // Intervalo que cruza 360 (ex: 350 a 20)
            return a >= start || a <= end;
        }
    }

    /** Helper para tocar um som para o jogador */
    private void playSound(SoundEvent sound, float volume, float pitch) {
        if (this.minecraft != null && this.minecraft.player != null) {
            // Usa playNotifySound para sons de UI que não precisam de posição no mundo
            this.minecraft.player.playNotifySound(sound, SoundSource.PLAYERS, volume, pitch);
        }
    }

    /** Helper para enviar uma mensagem na action bar do jogador */
    private void sendMessage(Component message) {
        if (this.minecraft != null && this.minecraft.player != null) {
            // true = overlay (action bar), false = chat
            this.minecraft.player.displayClientMessage(message, true);
        }
    }

    /** Fecha a tela e reseta o estado para interromper ticks */
    private void closeScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(null); // Define a tela atual como nula (fecha)
        }
        // Zera a fase para garantir que o método tick() pare de executar
        this.currentPhase = 0;
        this.ticksRemainingThisPhase = 0;
    }

} // Fim da classe GeradorScreen