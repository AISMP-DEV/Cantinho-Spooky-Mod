package net.aqualoco.cantinhospooky.block;

import net.aqualoco.cantinhospooky.CantinhoSpooky;
import net.aqualoco.cantinhospooky.block.entity.GeradorBlockEntity;
import net.aqualoco.cantinhospooky.network.GeradorPacket; // Importar o GeradorPacket correto
import net.aqualoco.cantinhospooky.network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock; // <-- Mudar para BaseEntityBlock
import net.minecraft.world.level.block.RenderShape;    // <-- Importar RenderShape
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition; // <-- Importar StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty; // <-- Importar BooleanProperty
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;


// 1. Mudar herança de Block para BaseEntityBlock
public class GeradorBlock extends BaseEntityBlock {

    // 2. Definir a Propriedade de Estado para indicar visualmente se precisa de reparo
    public static final BooleanProperty NEEDS_REPAIR = BooleanProperty.create("needs_repair");

    public GeradorBlock(Properties props) {
        super(props);
        // 3. Registrar o estado padrão do bloco (começa precisando de reparo)
        this.registerDefaultState(this.stateDefinition.any().setValue(NEEDS_REPAIR, true));
    }

    // 4. Sobrescrever createBlockStateDefinition para incluir nossa propriedade
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(NEEDS_REPAIR);
    }

    // 5. Definir como o bloco é renderizado (usamos um modelo JSON normal)
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // 6. Implementar o método que cria nosso BlockEntity
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeradorBlockEntity(pos, state);
    }

    // 7. Modificar drasticamente o método 'use' (interação com botão direito)
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        // Executa a lógica principal apenas no servidor
        if (!level.isClientSide()) {
            // Tenta obter o nosso BlockEntity na posição clicada
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof GeradorBlockEntity geradorBE) {

                // Consulta o BlockEntity para ver se o jogador pode tentar reparar
                if (geradorBE.canPlayerAttemptRepair(player)) {
                    // Se PODE tentar: envia o pacote para o cliente ABRIR a tela do QTE
                    // Note que agora passamos 'pos' para o pacote!
                    PacketHandler.CHANNEL.sendTo(
                            new GeradorPacket(pos), // <--- Passa a posição!
                            ((ServerPlayer) player).connection.connection,
                            net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT // Use a importação completa aqui se necessário
                    );
                    return InteractionResult.SUCCESS; // Interação bem-sucedida (iniciou QTE)

                } else {
                    // ---> ALTERAÇÃO AQUI <---
                    if (state.getValue(NEEDS_REPAIR)) {
                        // Usa a chave de tradução para cooldown
                        player.displayClientMessage(Component.translatable("message.cantinhospooky.gerador.cooldown"), true);
                    } else {
                        // Usa a chave de tradução para já reparado
                        player.displayClientMessage(Component.translatable("message.cantinhospooky.gerador.repaired"), true);
                    }
                    return InteractionResult.CONSUME; // Interação consumida (evita usar item da mão), mas não "bem-sucedida"
                }
            } else {
                // Caso MUITO raro onde o BlockEntity não existe ou é do tipo errado
                // Isso não deveria acontecer com BaseEntityBlock se registrado corretamente
                CantinhoSpooky.LOGGER.error("Erro ao obter GeradorBlockEntity em {}", pos);
                return InteractionResult.FAIL;
            }
        }

        // No lado do cliente, apenas sinaliza sucesso para dar o feedback visual (balançar a mão)
        // A tela só será aberta quando o pacote do servidor chegar.
        // Verificamos se precisa de reparo para não balançar a mão se já estiver consertado.
        return state.getValue(NEEDS_REPAIR) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    // --- Opcional: Lógica de Ticker (se necessário no futuro) ---
    /*
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Exemplo: Se precisássemos de um ticker no SERVIDOR:
        // if (level.isClientSide()) {
        //     return null; // Sem ticker no cliente
        // } else {
        //     // Retorna um ticker se o tipo for o nosso, chamando o método estático 'tick' do BE
        //     return createTickerHelper(type, ModBlockEntities.GERADOR_BE.get(), GeradorBlockEntity::tick);
        // }
        return null; // Sem ticker por enquanto
    }
    */
}