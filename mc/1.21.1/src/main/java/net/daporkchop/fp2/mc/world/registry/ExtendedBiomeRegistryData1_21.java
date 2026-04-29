package net.daporkchop.fp2.mc.world.registry;

import lombok.NonNull;
import net.daporkchop.fp2.core.world.registry.AbstractDenseExtendedBiomeRegistryData;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

public final class ExtendedBiomeRegistryData1_21 extends AbstractDenseExtendedBiomeRegistryData<Holder<Biome>> {
    public ExtendedBiomeRegistryData1_21(@NonNull GameRegistry1_21 registry) {
        super(registry);
    }
}
