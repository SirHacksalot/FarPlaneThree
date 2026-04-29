package net.daporkchop.fp2.mc.world.registry;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

public final class GameRegistry1_21 implements FGameRegistry {
    @SuppressWarnings("unchecked")
    private final Holder<Biome>[] idsToBiomes;
    private final Reference2IntMap<Holder<Biome>> biomesToIds;

    private final BlockState[] idsToStates;
    private final Reference2IntMap<BlockState> statesToIds;

    @Getter
    private final ExtendedBiomeRegistryData1_21 extendedBiomeRegistryData;
    @Getter
    private final ExtendedStateRegistryData1_21 extendedStateRegistryData;

    @SuppressWarnings("unchecked")
    public GameRegistry1_21(@NonNull RegistryAccess registryAccess) {
        Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);

        this.idsToBiomes = biomeRegistry.holders()
                .sorted(Comparator.comparingInt(h -> biomeRegistry.getId(h.value())))
                .toArray(Holder[]::new);

        this.biomesToIds = new Reference2IntOpenHashMap<>(this.idsToBiomes.length);
        this.biomesToIds.defaultReturnValue(-1);
        IntStream.range(0, this.idsToBiomes.length).forEach(id ->
                checkState(this.biomesToIds.putIfAbsent(this.idsToBiomes[id], id) < 0,
                        "duplicate biome: %s", this.idsToBiomes[id]));

        this.extendedBiomeRegistryData = new ExtendedBiomeRegistryData1_21(this);

        this.idsToStates = StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(), false)
                .flatMap(block -> {
                    Stream<BlockState> allStates = Stream.of(block.defaultBlockState());
                    for (Property<?> property : block.getStateDefinition().getProperties()) {
                        allStates = allStates.flatMap(state ->
                                property.getPossibleValues().stream().map(value -> state.setValue(property, uncheckedCast(value))));
                    }
                    return allStates;
                })
                .toArray(BlockState[]::new);

        this.statesToIds = new Reference2IntOpenHashMap<>(this.idsToStates.length);
        this.statesToIds.defaultReturnValue(-1);
        IntStream.range(0, this.idsToStates.length).forEach(id ->
                checkState(this.statesToIds.putIfAbsent(this.idsToStates[id], id) < 0,
                        "duplicate state: %s", this.idsToStates[id]));

        this.extendedStateRegistryData = new ExtendedStateRegistryData1_21(this);
    }

    @Override
    public byte[] registryToken() {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        try {
            buf.writeCharSequence(this.getClass().getTypeName(), StandardCharsets.UTF_8);
            buf.writeByte(0);

            for (Holder<Biome> biome : this.idsToBiomes) {
                buf.writeCharSequence(biome.unwrapKey().get().location().toString(), StandardCharsets.UTF_8);
                buf.writeByte(0);
            }
            for (BlockState state : this.idsToStates) {
                buf.writeCharSequence(state.toString(), StandardCharsets.UTF_8);
                buf.writeByte(0);
            }

            byte[] arr = new byte[buf.readableBytes()];
            buf.readBytes(arr);
            return arr;
        } finally {
            buf.release();
        }
    }

    @Override
    public int biomesCount() {
        return this.idsToBiomes.length;
    }

    @Override
    public int biome2id(@NonNull Object biome) throws UnsupportedOperationException, ClassCastException {
        return this.biomesToIds.getInt((Holder<Biome>) biome);
    }

    @Override
    public Holder<Biome> id2biome(int biome) throws UnsupportedOperationException {
        return this.idsToBiomes[biome];
    }

    @Override
    public int statesCount() {
        return this.idsToStates.length;
    }

    @Override
    public int state2id(@NonNull Object state) throws UnsupportedOperationException, ClassCastException {
        return this.statesToIds.getInt((BlockState) state);
    }

    @Override
    public int state2id(@NonNull Object block, int meta) throws UnsupportedOperationException, ClassCastException {
        throw new UnsupportedOperationException();
    }

    @Override
    public BlockState id2state(int state) throws UnsupportedOperationException {
        return this.idsToStates[state];
    }
}
