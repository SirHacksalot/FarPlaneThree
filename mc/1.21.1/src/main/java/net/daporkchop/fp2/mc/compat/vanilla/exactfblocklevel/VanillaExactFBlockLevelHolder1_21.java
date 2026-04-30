package net.daporkchop.fp2.mc.compat.vanilla.exactfblocklevel;

//? if neoforge {
import com.mojang.serialization.Codec;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.core.minecraft.util.threading.asynccache.AsyncCacheNBT;
import net.daporkchop.fp2.core.minecraft.world.chunks.AbstractChunksExactFBlockLevelHolder;
import net.daporkchop.fp2.core.minecraft.world.chunks.AbstractPrefetchedChunksExactFBlockLevel;
import net.daporkchop.fp2.core.server.event.ColumnSavedEvent;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.fp2.core.util.datastructure.Datastructures;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSegtreeSet;
import net.daporkchop.fp2.mc.server.world.level.FLevelServer1_21;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.daporkchop.lib.math.vector.Vec2i;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


public class VanillaExactFBlockLevelHolder1_21 extends AbstractChunksExactFBlockLevelHolder<OffThreadChunk1_21> {
    public VanillaExactFBlockLevelHolder1_21(@NonNull FLevelServer1_21 level) {
        super(level, 4); // chunkShift=4 → 16-block chunks
    }

    @Override
    protected NDimensionalIntSegtreeSet createChunksExistIndex(@NonNull IFarLevelServer level) {
        ServerLevel serverLevel = (ServerLevel) level.implLevel();
        return Datastructures.INSTANCE.nDimensionalIntSegtreeSet()
                .dimensions(2)
                .threadSafe(true)
                .initialPoints(() -> {
                    try {
                        Field f = net.minecraft.server.MinecraftServer.class.getDeclaredField("storageSource");
                        f.setAccessible(true);
                        LevelStorageSource.LevelStorageAccess storageAccess =
                                (LevelStorageSource.LevelStorageAccess) f.get(serverLevel.getServer());
                        Path regionDir = storageAccess.getDimensionPath(serverLevel.dimension()).resolve("region");
                        if (!Files.isDirectory(regionDir)) {
                            return Stream.empty();
                        }
                        return Files.list(regionDir)
                                .filter(p -> p.getFileName().toString().endsWith(RegionFileStorage.ANVIL_EXTENSION))
                                .flatMap(regionFile -> {
                                    String name = regionFile.getFileName().toString();
                                    String[] parts = name.split("\\.");
                                    if (parts.length != 4 || !parts[0].equals("r")) return Stream.empty();
                                    int regionX, regionZ;
                                    try {
                                        regionX = Integer.parseInt(parts[1]);
                                        regionZ = Integer.parseInt(parts[2]);
                                    } catch (NumberFormatException e) {
                                        return Stream.empty();
                                    }
                                    List<int[]> found = new ArrayList<>();
                                    try (RandomAccessFile raf = new RandomAccessFile(regionFile.toFile(), "r")) {
                                        if (raf.length() < 4096) return found.stream();
                                        byte[] header = new byte[4096];
                                        raf.readFully(header);
                                        ByteBuffer buf = ByteBuffer.wrap(header);
                                        for (int cz = 0; cz < 32; cz++) {
                                            for (int cx = 0; cx < 32; cx++) {
                                                if (buf.getInt((cz * 32 + cx) * 4) != 0) {
                                                    found.add(new int[]{regionX * 32 + cx, regionZ * 32 + cz});
                                                }
                                            }
                                        }
                                    } catch (IOException e) {
                                        ((FLevelServer1_21) level).fp2().log().warn("Failed to scan region file: " + regionFile, e);
                                    }
                                    return found.stream();
                                })
                                .parallel();
                    } catch (Exception e) {
                        ((FLevelServer1_21) level).fp2().log().error("Failed to scan region directory for chunk index", e);
                        return Stream.empty();
                    }
                })
                .build();
    }

    @Override
    protected AsyncCacheNBT<Vec2i, ?, OffThreadChunk1_21, ?> createChunkCache(@NonNull IFarLevelServer level) {
        ServerLevel serverLevel = (ServerLevel) level.implLevel();
        Registry<Biome> biomeRegistry = serverLevel.registryAccess().registryOrThrow(Registries.BIOME);
        Codec<PalettedContainerRO<Holder<Biome>>> biomeCodec = OffThreadChunk1_21.createBiomeCodec(biomeRegistry);
        return new ChunkCache((FLevelServer1_21) level, serverLevel, biomeCodec);
    }

    @Override
    protected AbstractPrefetchedChunksExactFBlockLevel<OffThreadChunk1_21> prefetchedWorld(boolean generationAllowed, @NonNull List<OffThreadChunk1_21> chunks) {
        return new PrefetchedChunksFBlockLevel1_21(this, generationAllowed, chunks);
    }

    @Override
    @FEventHandler
    protected void onColumnSaved(@NonNull ColumnSavedEvent event) {
        if (event.column().isFullyPopulated()) {
            super.onColumnSaved(event);
        }
    }

    @RequiredArgsConstructor
    protected static class ChunkCache extends AsyncCacheNBT<Vec2i, Object, OffThreadChunk1_21, CompoundTag> {
        @NonNull
        private final FLevelServer1_21 level;
        @NonNull
        private final ServerLevel serverLevel;
        @NonNull
        private final Codec<PalettedContainerRO<Holder<Biome>>> biomeCodec;

        private boolean isFullyGeneratedChunk(@NonNull CompoundTag nbt) {
            return ChunkSerializer.getChunkTypeFromTag(nbt) == net.minecraft.world.level.chunk.status.ChunkType.LEVELCHUNK
                    && nbt.getByte(ChunkSerializer.IS_LIGHT_ON_TAG) != 0;
        }

        @Override
        protected OffThreadChunk1_21 parseNBT(@NonNull Vec2i key, @NonNull Object param, @NonNull CompoundTag nbt) {
            return this.isFullyGeneratedChunk(nbt)
                    ? new OffThreadChunk1_21(this.serverLevel, this.biomeCodec, nbt)
                    : null;
        }

        @Override
        protected OffThreadChunk1_21 loadFromDisk(@NonNull Vec2i key, @NonNull Object param) {
            Optional<CompoundTag> nbtOpt = this.serverLevel.getChunkSource().chunkMap
                    .read(new ChunkPos(key.x(), key.y()))
                    .join();
            return nbtOpt.map(nbt -> this.parseNBT(key, param, nbt)).orElse(null);
        }

        @Override
        protected void triggerGeneration(@NonNull Vec2i key, @NonNull Object param) {
            int x = key.x();
            int z = key.y();
            this.level.world().serverExecutor().run(() -> {
                // Force chunk to full status (generates it if not yet generated)
                this.serverLevel.getChunkSource().getChunk(x, z, ChunkStatus.FULL, true);
                // Save all dirty chunks to disk
                this.serverLevel.getChunkSource().save(true);
            }).join();
            // Flush the IO worker to ensure the write completes
            try {
                this.serverLevel.getChunkSource().chunkMap.flushWorker();
            } catch (Exception e) {
                this.level.fp2().log().error("Failed to flush chunk IO worker", e);
            }
        }
    }
}
//?}
