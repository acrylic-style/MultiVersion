package xyz.acrylicstyle.multiVersion.transformer.rewriters;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.ConnectionProtocol;
import org.jetbrains.annotations.NotNull;
import xyz.acrylicstyle.multiVersion.transformer.PacketRewriter;
import xyz.acrylicstyle.multiVersion.transformer.PacketWrapper;
import xyz.acrylicstyle.multiVersion.transformer.TransformableProtocolVersions;

import java.util.Objects;

public class S22w06a_To_S22w05a extends PacketRewriter {
    public S22w06a_To_S22w05a() {
        this(TransformableProtocolVersions.SNAPSHOT_22W06A, TransformableProtocolVersions.SNAPSHOT_22W05A);
    }

    protected S22w06a_To_S22w05a(@NotNull TransformableProtocolVersions sourcePV, @NotNull TransformableProtocolVersions targetPV) {
        super(sourcePV, targetPV);
    }

    @Override
    public void registerInbound() {
        // Join Game
        rewriteInbound(ConnectionProtocol.PLAY, 0x26, wrapper -> {
            wrapper.passthrough(PacketWrapper.Type.INT); // Entity ID
            wrapper.passthrough(PacketWrapper.Type.BOOLEAN); // Is hardcore
            wrapper.passthrough(PacketWrapper.Type.BYTE); // Game mode
            wrapper.passthrough(PacketWrapper.Type.BYTE); // Previous game mode
            wrapper.passthroughCollection(PacketWrapper.Type.RESOURCE_LOCATION); // World count / World names

            // Dimension Codec
            CompoundTag registry = wrapper.readNbt();
            Objects.requireNonNull(registry, "dimension registry is null!");
            CompoundTag dimensionsHolder = registry.getCompound("minecraft:dimension_type");
            if (dimensionsHolder.get("value") instanceof ListTag dimensions) {
                for (Tag dimension : dimensions) {
                    addTagPrefix(((CompoundTag) dimension).getCompound("element"));
                }
            }
            wrapper.writeNbt(registry);

            // Dimension
            CompoundTag dimension = wrapper.readNbt();
            addTagPrefix(Objects.requireNonNull(dimension, "dimension is null!"));
            wrapper.writeNbt(dimension);

            wrapper.passthrough(PacketWrapper.Type.RESOURCE_LOCATION); // World name
            wrapper.passthrough(PacketWrapper.Type.LONG); // Hashed seed
            wrapper.passthrough(PacketWrapper.Type.VAR_INT); // Max players
            wrapper.passthrough(PacketWrapper.Type.VAR_INT); // View distance
            wrapper.passthrough(PacketWrapper.Type.VAR_INT); // Simulation distance
            wrapper.passthrough(PacketWrapper.Type.BOOLEAN); // Reduced debug info
            wrapper.passthrough(PacketWrapper.Type.BOOLEAN); // Enable respawn screen
            wrapper.passthrough(PacketWrapper.Type.BOOLEAN); // Is debug
            wrapper.passthrough(PacketWrapper.Type.BOOLEAN); // Is flat
        });

        // Respawn
        rewriteInbound(ConnectionProtocol.PLAY, 0x3D, wrapper -> {
            var tag = wrapper.readNbt();
            addTagPrefix(Objects.requireNonNull(tag, "dimension is null!"));
            wrapper.writeNbt(tag);
            wrapper.passthroughAll();
        });
    }

    @Override
    public void registerOutbound() {
    }

    private static void addTagPrefix(CompoundTag tag) {
        Tag infiniburnTag = tag.get("infiniburn");
        if (infiniburnTag instanceof StringTag infiniburn) {
            tag.put("infiniburn", StringTag.valueOf("#" + infiniburn.getAsString()));
        }
    }
}
