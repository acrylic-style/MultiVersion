package xyz.acrylicstyle.multiVersion.data;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum MappingDataKey {
    BLOCK_STATE(null),
    ITEM("minecraft:item"),
    BLOCK("minecraft:block"),
    ENTITY_TYPE("minecraft:entity_type"),
    SOUND_EVENT("minecraft:sound_event"),
    PARTICLE_TYPE("minecraft:particle_type"),
    ;

    private final ResourceLocation registry;

    MappingDataKey(@Nullable String registry) {
        if (registry == null) {
            this.registry = null;
        } else {
            this.registry = new ResourceLocation(registry);
        }
    }

    @Contract(pure = true)
    @Nullable
    public ResourceLocation registry() {
        return registry;
    }

    @Contract(pure = true)
    @NotNull
    public ResourceLocation registryOrThrow() {
        var registry = registry();
        if (registry == null) {
            throw new IllegalArgumentException("No registry found for " + this);
        }
        return registry;
    }
}
