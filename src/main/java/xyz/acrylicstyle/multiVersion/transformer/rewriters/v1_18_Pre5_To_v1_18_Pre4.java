package xyz.acrylicstyle.multiVersion.transformer.rewriters;

import xyz.acrylicstyle.multiVersion.transformer.TransformableProtocolVersions;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagNetworkSerialization;
import org.jetbrains.annotations.NotNull;

public class v1_18_Pre5_To_v1_18_Pre4 extends S22w06a_To_S22w05a {
    public v1_18_Pre5_To_v1_18_Pre4() {
        this(TransformableProtocolVersions.v1_18_PRE5, TransformableProtocolVersions.v1_18_PRE4);
    }

    protected v1_18_Pre5_To_v1_18_Pre4(@NotNull TransformableProtocolVersions sourcePV, @NotNull TransformableProtocolVersions targetPV) {
        super(sourcePV, targetPV);
    }

    @Override
    public void registerInbound() {
        // ClientboundUpdateTagsPacket
        rewriteInbound(ConnectionProtocol.PLAY, 0x67, wrapper -> {
            var tags = wrapper.readMap((buf) -> ResourceKey.createRegistryKey(buf.readResourceLocation()), TagNetworkSerialization.NetworkPayload::read);
            wrapper.writeVarInt(tags.size());
            tags.forEach((key, value) -> {
                wrapper.writeResourceLocation(key.location());
                if (key.location().getNamespace().equals("minecraft") && key.location().getPath().equals("block")) {
                    String[] newTags = {
                            "azalea_root_replaceable",
                            "terracotta",
                            "azalea_grows_on",
                            "replaceable_plants"
                    };
                    addEmptyTags(value, newTags);
                } else if (key.location().getNamespace().equals("minecraft") && key.location().getPath().equals("item")) {
                    String[] newTags = {
                            "terracotta",
                            "dirt",
                    };
                    addEmptyTags(value, newTags);
                }
                value.write(wrapper.getWrite());
            });
        });
    }

    @Override
    public void registerOutbound() {
    }
}
