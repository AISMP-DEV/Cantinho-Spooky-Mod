package net.aqualoco.cantinhospooky.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel; // Importado para partículas/efeitos futuros
import net.minecraft.server.level.ServerPlayer; // Importado para efeitos futuros
import net.minecraft.world.effect.MobEffectInstance; // Para efeitos futuros
import net.minecraft.world.effect.MobEffects;      // Para efeitos futuros
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import net.aqualoco.cantinhospooky.Config;

// Importações para Partículas/Efeitos (Exemplo)
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import java.util.List;


public class GeradorBlockEntity extends BlockEntity {

    // --- Constantes ---
    //private static final int COOLDOWN_TICKS = 20 * 20; // 20 segundos * 20 ticks/segundo
    private static final String NBT_NEEDS_REPAIR = "NeedsRepair";
    // Novo NBT para cooldown global
    private static final String NBT_GLOBAL_COOLDOWN_EXPIRY = "GlobalCooldownExpiry";

    // --- Estado Interno ---
    private boolean needsRepair = true; // Começa precisando de reparo
    // Tempo (tick do jogo) em que o cooldown GLOBAL termina. 0 = sem cooldown.
    private long globalCooldownExpiryTick = 0L;


    // --- Construtor ---
    public GeradorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GERADOR_BE.get(), pos, state);
    }

    // --- Lógica Principal ---

    /** Verifica se o bloco precisa de reparo E se NÃO está em cooldown global. */
    public boolean canPlayerAttemptRepair(Player player) {
        // A verificação agora é só no estado 'needsRepair' e no tempo global
        // Não precisamos mais verificar o jogador específico ou limpar mapas
        return this.needsRepair && (this.level != null && this.level.getGameTime() >= this.globalCooldownExpiryTick);
    }

    /** Retorna true se o bloco atualmente precisa de reparo. */
    public boolean needsRepair() {
        return this.needsRepair;
    }

    /** Marca o bloco como reparado com sucesso. */
    public void setRepaired() {
        if (this.needsRepair) {
            this.needsRepair = false;
            this.globalCooldownExpiryTick = 0L; // Garante que o cooldown global seja resetado
            // TODO: Futuramente, mudar o BlockState aqui para refletir visualmente (e remover fumaça?)
            setChangedAndSync();
            // Exemplo: Remover fumaça visualmente (requer estado no BlockState)
            // if(this.level instanceof ServerLevel serverLevel) { ... }
        }
    }

    /**
     * Inicia o cooldown global do bloco.
     * Opcionalmente, pode receber o jogador que falhou para aplicar efeitos nele.
     */
    public void startGlobalCooldown(@Nullable Player failedPlayer) {
        if (this.level == null || this.level.isClientSide()) return; // Apenas no servidor

        // Define o tempo de expiração do cooldown global
        // Usa o valor da Config em vez da constante local
        this.globalCooldownExpiryTick = this.level.getGameTime() + Config.globalCooldownTicks;
        setChangedAndSync(); // Salva e sincroniza (para que clientes possam talvez ver estado de cooldown)

        // --- Lógica para Efeitos Futuros (Exemplos) ---
        ServerLevel serverLevel = (ServerLevel) this.level;
        BlockPos pos = getBlockPos();

        // 1. Efeito de Fumaça (Exemplo simples - idealmente ligado a um BlockState)
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, // Tipo de partícula
                pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, // Posição (topo do bloco)
                15, // Contagem
                0.2, 0.1, 0.2, // Variação XYZ
                0.01); // Velocidade

        // 2. Efeito no Jogador que Falhou (se fornecido)
        if (failedPlayer instanceof ServerPlayer serverPlayer) {
            serverPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Config.globalCooldownTicks, 0));
            // serverPlayer.addEffect(new MobEffectInstance(MobEffects.CONFUSION, COOLDOWN_TICKS / 2, 0)); // Ex: Náusea
        }

        // 3. Efeito em Área (Exemplo: Lentidão para jogadores próximos)
        // AABB area = new AABB(pos).inflate(3.0); // Caixa de 3 blocos ao redor
        // List<ServerPlayer> playersNearby = serverLevel.getEntitiesOfClass(ServerPlayer.class, area, p -> p != failedPlayer); // Pega jogadores na área (exceto quem falhou)
        // for(ServerPlayer nearbyPlayer : playersNearby) {
        //     nearbyPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1)); // Aplica efeito curto
        // }

        // Tocar um som de falha aqui também seria bom
        // serverLevel.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.0f, 0.8f);

    }


    // --- Persistência (Salvar/Carregar NBT) ---

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        nbt.putBoolean(NBT_NEEDS_REPAIR, this.needsRepair);
        // Salva o tempo de expiração do cooldown global
        nbt.putLong(NBT_GLOBAL_COOLDOWN_EXPIRY, this.globalCooldownExpiryTick);
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.needsRepair = nbt.getBoolean(NBT_NEEDS_REPAIR);
        // Carrega o tempo de expiração do cooldown global
        this.globalCooldownExpiryTick = nbt.getLong(NBT_GLOBAL_COOLDOWN_EXPIRY);

        // Verifica se o cooldown carregado já expirou (importante se o jogo ficou fechado)
        if (this.level != null && this.level.getGameTime() >= this.globalCooldownExpiryTick) {
            this.globalCooldownExpiryTick = 0L; // Reseta se já expirou
        }
    }

    // --- Sincronização Cliente/Servidor ---
    // (getUpdateTag, handleUpdateTag, getUpdatePacket, setChangedAndSync permanecem os mesmos)
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag nbt = new CompoundTag();
        saveAdditional(nbt);
        return nbt;
    }
    @Override
    public void handleUpdateTag(CompoundTag tag) { load(tag); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    protected void setChangedAndSync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
}