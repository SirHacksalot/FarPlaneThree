package net.daporkchop.fp2.mc.server.world;

//? if neoforge {
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.server.world.FColumn;
import net.daporkchop.lib.math.vector.Vec2i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;

@RequiredArgsConstructor
public class FColumn1_21 implements FColumn {
    @NonNull
    private final ChunkPos pos;
    @NonNull
    private final CompoundTag nbt;

    @Override
    public Vec2i pos() {
        return Vec2i.of(this.pos.x, this.pos.z);
    }

    @Override
    public boolean isFullyPopulated() {
        return ChunkSerializer.getChunkTypeFromTag(this.nbt) == ChunkType.LEVELCHUNK
                && this.nbt.getByte(ChunkSerializer.IS_LIGHT_ON_TAG) != 0;
    }
}
//?}
