package xyz.acrylicstyle.multiVersion.transformer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

// new -> old
public class PacketRewriter {
    /**
     * @deprecated Should only be used for debugging
     */
    @Deprecated
    protected static final Logger LOGGER = LogManager.getLogger();
    private final int sourcePV;
    private final int targetPV;
    private final Int2ObjectMap<Int2IntMap> remapInbounds = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<Int2IntMap> remapOutbounds = new Int2ObjectOpenHashMap<>();
    // Map<ConnectionProtocol, Map<packet_id, List<packet_rewriter>>
    private final Int2ObjectMap<Int2ObjectMap<List<PacketConsumer>>> rewriteInbounds = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<Int2ObjectMap<List<PacketConsumer>>> rewriteOutbounds = new Int2ObjectOpenHashMap<>();
    private final ThreadLocal<Stack<List<ByteBuf>>> writtenPackets = ThreadLocal.withInitial(ObjectArrayList::new);
    private boolean registeringInbound;
    private boolean registeringOutbound;

    /**
     * @param sourcePV source protocol version (pv before rewrite)
     * @param targetPV target protocol version (pv after rewrite)
     */
    protected PacketRewriter(int sourcePV, int targetPV) {
        this.sourcePV = sourcePV;
        this.targetPV = targetPV;
    }

    protected PacketRewriter(@NotNull TransformableProtocolVersions sourcePV, @NotNull TransformableProtocolVersions targetPV) {
        this(sourcePV.getProtocolVersion(), targetPV.getProtocolVersion());
    }

    protected void preRegister() {}

    public final void register() {
        remapInbounds.clear();
        remapOutbounds.clear();
        rewriteInbounds.clear();
        rewriteOutbounds.clear();
        preRegister();
        registeringInbound = true;
        preRegisterInbound();
        registerInbound();
        registeringInbound = false;
        registeringOutbound = true;
        preRegisterOutbound();
        registerOutbound();
        registeringOutbound = false;
    }

    @NotNull
    public static PacketRewriter of(int sourcePV, int targetPV) {
        return new PacketRewriter(sourcePV, targetPV);
    }

    @NotNull
    public static PacketRewriter of(@NotNull TransformableProtocolVersions sourcePV, @NotNull TransformableProtocolVersions targetPV) {
        return of(sourcePV.getProtocolVersion(), targetPV.getProtocolVersion());
    }

    public int getSourcePV() {
        return sourcePV;
    }

    public int getTargetPV() {
        return targetPV;
    }

    protected void preRegisterInbound() {
    }

    protected void preRegisterOutbound() {
        // ClientIntentionPacket (server-bound)
        // this implementation just changes the protocol version
        rewriteOutbound(ConnectionProtocol.HANDSHAKING, 0x00, wrapper -> {
            wrapper.readVarInt(); // Protocol Version
            wrapper.writeVarInt(getTargetPV()); // Protocol Version
            wrapper.passthroughAll();
        });
    }

    protected void registerSoundRewriter() {
        registerSoundRewriter(0x5C, 0x5D);
    }

    protected void registerSoundRewriter(int soundEntityPacketId, int soundPacketId) {
        // ClientboundSoundEntityPacket
        internalRewrite(rewriteInbounds, ConnectionProtocol.PLAY, soundEntityPacketId, wrapper -> {
            wrapper.writeVarInt(remapSoundId(wrapper.readVarInt()));
            wrapper.passthroughAll();
        });
        // ClientboundSoundPacket
        internalRewrite(rewriteInbounds, ConnectionProtocol.PLAY, soundPacketId, wrapper -> {
            wrapper.writeVarInt(remapSoundId(wrapper.readVarInt()));
            wrapper.passthroughAll();
        });
    }

    protected void registerItemRewriter() {
        registerItemRewriter(0x0A, 0x2A, 0x12, 0x14, 0x26, 0x4D, 0x50, 0x64, 0x67);
    }

    protected void registerItemRewriter(int... ids) {
        // TODO: These code WILL break when packet structure is changed
        internalRewrite(rewriteOutbounds, ConnectionProtocol.PLAY, ids[0], wrapper -> wrapper.readIsPassthrough(() -> new ServerboundContainerClickPacket(wrapper)));
        internalRewrite(rewriteOutbounds, ConnectionProtocol.PLAY, ids[1], wrapper -> wrapper.readIsPassthrough(() -> new ServerboundSetCreativeModeSlotPacket(wrapper)));
        internalRewrite(rewriteInbounds, ConnectionProtocol.PLAY, ids[2], wrapper -> wrapper.readIsPassthrough(() -> new ClientboundContainerSetContentPacket(wrapper)));
        internalRewrite(rewriteInbounds, ConnectionProtocol.PLAY, ids[3], wrapper -> wrapper.readIsPassthrough(() -> new ClientboundContainerSetSlotPacket(wrapper)));
        internalRewrite(rewriteInbounds, ConnectionProtocol.PLAY, ids[4], wrapper -> wrapper.readIsPassthrough(() -> new ClientboundMerchantOffersPacket(wrapper)));
        internalRewrite(rewriteInbounds, ConnectionProtocol.PLAY, ids[5], wrapper -> wrapper.readIsPassthrough(() -> new ClientboundSetEntityDataPacket(wrapper)));
        internalRewrite(rewriteInbounds, ConnectionProtocol.PLAY, ids[6], wrapper -> wrapper.readIsPassthrough(() -> new ClientboundSetEquipmentPacket(wrapper)));
        internalRewrite(rewriteInbounds, ConnectionProtocol.PLAY, ids[7], wrapper -> wrapper.readIsPassthrough(() -> new ClientboundUpdateAdvancementsPacket(wrapper)));
        internalRewrite(rewriteInbounds, ConnectionProtocol.PLAY, ids[8], wrapper -> wrapper.readIsPassthrough(() -> new ClientboundUpdateRecipesPacket(wrapper)));
    }

    protected void registerParticleRewriter() {
        registerParticleRewriter(0x22);
    }

    protected void registerParticleRewriter(int packetId) {
        // ClientboundLevelParticlesPacket
        internalRewrite(rewriteInbounds, ConnectionProtocol.PLAY, packetId, wrapper -> {
            wrapper.writeInt(remapParticleId(wrapper.readInt()));
            wrapper.passthroughAll();
        });
    }

    protected void registerEntityRewriter() {
        registerEntityRewriter(0x00);
    }

    protected void registerEntityRewriter(int cSpawnEntity) {
        internalRewrite(rewriteInbounds, ConnectionProtocol.PLAY, cSpawnEntity, wrapper -> {
            wrapper.passthroughVarInt(); // Entity ID
            wrapper.passthroughUUID(); // Object UUID
            wrapper.writeVarInt(remapEntityType(wrapper.readVarInt()));
            wrapper.passthroughAll();
        });
    }

    protected int remapSoundId(int soundId) {
        return soundId;
    }

    protected int remapParticleId(int particleId) {
        return particleId;
    }

    protected int remapEntityType(int entityType) {
        return entityType;
    }

    @NotNull
    protected ItemStack rewriteOutboundItemData(@NotNull PacketWrapper wrapper) {
        return passthroughItemData(wrapper);
    }

    @NotNull
    protected ItemStack rewriteInboundItemData(@NotNull PacketWrapper wrapper) {
        return passthroughItemData(wrapper);
    }

    @NotNull
    protected final ItemStack passthroughItemData(@NotNull PacketWrapper wrapper) {
        if (wrapper.passthroughBoolean()) { // present
            var id = wrapper.passthroughVarInt();
            var count = wrapper.passthroughByte();
            var tag = wrapper.passthroughNbt();
            ItemStack item = new ItemStack(Item.byId(id), count);
            item.setTag(tag);
            return item;
        } else {
            return ItemStack.EMPTY;
        }
    }

    public void registerInbound() {}

    public void registerOutbound() {}

    protected final void remapInbound(@NotNull ConnectionProtocol protocol, int oldId, int newId) {
        if (!registeringInbound) throw new IllegalStateException("Not registering inbound");
        remapInbounds.computeIfAbsent(protocol.ordinal(), (k) -> new Int2IntOpenHashMap()).put(oldId, newId);
    }

    protected final void remapOutbound(@NotNull ConnectionProtocol protocol, int oldId, int newId) {
        if (!registeringOutbound) throw new IllegalStateException("Not registering outbound");
        remapOutbounds.computeIfAbsent(protocol.ordinal(), (k) -> new Int2IntOpenHashMap()).put(oldId, newId);
    }

    /**
     * @param protocol the protocol
     * @param newId packet id of newer version
     * @return packet id of older version
     */
    public final int getInboundId(@NotNull ConnectionProtocol protocol, int newId) {
        if (!remapInbounds.containsKey(protocol.ordinal())) return newId;
        return remapInbounds.get(protocol.ordinal()).getOrDefault(newId, newId);
    }

    /**
     * @param protocol the protocol
     * @param newId packet id of newer version
     * @return packet id of older version
     */
    public final int getOutboundId(@NotNull ConnectionProtocol protocol, int newId) {
        if (!remapOutbounds.containsKey(protocol.ordinal())) return newId;
        return remapOutbounds.get(protocol.ordinal()).getOrDefault(newId, newId);
    }

    /**
     * Rewrites an inbound packet.
     * @param protocol the protocol
     * @param oldId the (post-remap) packet id to rewrite
     * @param handler the handler
     */
    protected final void rewriteInbound(@NotNull ConnectionProtocol protocol, int oldId, @NotNull PacketConsumer handler) {
        if (!registeringInbound) throw new IllegalStateException("Not registering inbound");
        internalRewrite(rewriteInbounds, protocol, oldId, handler);
    }

    /**
     * Rewrites an outbound packet.
     * @param protocol the protocol
     * @param oldId the (post-remap) packet id to rewrite
     * @param handler the handler
     */
    protected final void rewriteOutbound(@NotNull ConnectionProtocol protocol, int oldId, @NotNull PacketConsumer handler) {
        if (!registeringOutbound) throw new IllegalStateException("Not registering outbound");
        internalRewrite(rewriteOutbounds, protocol, oldId, handler);
    }

    protected final void internalRewrite(@NotNull Int2ObjectMap<Int2ObjectMap<List<PacketConsumer>>> map, @NotNull ConnectionProtocol protocol, int oldId, @NotNull PacketConsumer handler) {
        map.computeIfAbsent(protocol.ordinal(), (k) -> new Int2ObjectOpenHashMap<>(ConnectionProtocol.values().length))
                .computeIfAbsent(oldId, (k) -> new ObjectArrayList<>())
                .add(handler);
    }

    protected final void writeInboundPacket(@NotNull ConnectionProtocol protocol, int packetId, @NotNull Consumer<FriendlyByteBuf> handler) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(packetId);
        handler.accept(buf);
        writtenPackets.get().top().addAll(PacketRewriterManager.rewriteInbound(protocol, buf, targetPV, true));
    }

    protected final void writeOutboundPacket(@NotNull ConnectionProtocol protocol, int packetId, @NotNull Consumer<FriendlyByteBuf> handler) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(packetId);
        handler.accept(buf);
        writtenPackets.get().top().addAll(PacketRewriterManager.rewriteOutbound(protocol, buf, targetPV, true));
    }

    @NotNull
    public final List<ByteBuf> doRewriteInbound(@NotNull ConnectionProtocol protocol, int oldId, @NotNull PacketWrapper wrapper) {
        PacketWrapperRewriter packetWrapperRewriter = new PacketWrapperRewriter(wrapper, this::rewriteInboundItemData);
        return doRewrite(protocol, oldId, packetWrapperRewriter, rewriteInbounds);
    }

    @NotNull
    public final List<ByteBuf> doRewriteOutbound(@NotNull ConnectionProtocol protocol, int oldId, @NotNull PacketWrapper wrapper) {
        PacketWrapperRewriter packetWrapperRewriter = new PacketWrapperRewriter(wrapper, this::rewriteOutboundItemData);
        return doRewrite(protocol, oldId, packetWrapperRewriter, rewriteOutbounds);
    }

    @NotNull
    private List<ByteBuf> doRewrite(@NotNull ConnectionProtocol protocol, int oldId, @NotNull PacketWrapper wrapper, Int2ObjectMap<Int2ObjectMap<List<PacketConsumer>>> map) {
        if (!map.containsKey(protocol.ordinal())) {
            wrapper.passthroughAll();
            return ObjectList.of();
        }
        Collection<PacketConsumer> consumers = map.get(protocol.ordinal()).get(oldId);
        if (consumers == null || consumers.isEmpty()) {
            wrapper.passthroughAll();
            return ObjectList.of();
        }
        writtenPackets.get().push(new ObjectArrayList<>());
        List<ByteBuf> list;
        try {
            for (PacketConsumer consumer : consumers) {
                consumer.accept(wrapper);
            }
        } finally {
            list = writtenPackets.get().pop();
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public final Map<ResourceLocation, IntList> getTags(@NotNull TagNetworkSerialization.NetworkPayload networkPayload) {
        try {
            Field f = TagNetworkSerialization.NetworkPayload.class.getDeclaredField("tags");
            f.setAccessible(true);
            return (Map<ResourceLocation, IntList>) f.get(networkPayload);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    public final void addEmptyTags(@NotNull TagNetworkSerialization.NetworkPayload networkPayload, @NotNull ResourceLocation@NotNull... locations) {
        Map<ResourceLocation, IntList> map = getTags(networkPayload);
        for (ResourceLocation location : locations) {
            map.put(location, IntLists.emptyList());
        }
    }

    public final void addEmptyTags(@NotNull TagNetworkSerialization.NetworkPayload networkPayload, @NotNull String@NotNull... locations) {
        Map<ResourceLocation, IntList> map = getTags(networkPayload);
        for (String location : locations) {
            map.put(new ResourceLocation(location), IntLists.emptyList());
        }
    }

    public final void addTags(@NotNull TagNetworkSerialization.NetworkPayload networkPayload, @NotNull String location, int@NotNull... ids) {
        Map<ResourceLocation, IntList> map = getTags(networkPayload);
        map.put(new ResourceLocation(location), IntList.of(ids));
    }

    public static class PacketWrapperRewriter extends PacketWrapper {
        private final PacketWrapper wrapper;
        private final Function<PacketWrapper, ItemStack> rewriteItem;

        public PacketWrapperRewriter(@NotNull PacketWrapper wrapper, @NotNull Function<PacketWrapper, ItemStack> rewriteItem) {
            super(wrapper.getRead(), wrapper.getWrite());
            this.wrapper = wrapper;
            this.rewriteItem = rewriteItem;
        }

        @Override
        public void cancel() {
            wrapper.cancel();
        }

        @Override
        public boolean isCancelled() {
            return wrapper.isCancelled();
        }

        @Override
        public @NotNull ItemStack passthroughItem() {
            return rewriteItem.apply(this);
        }

        @Override
        public @NotNull PacketWrapper passthrough(@NotNull Type type) {
            if (type == Type.ITEM) {
                rewriteItem.apply(this);
                return this;
            }
            return super.passthrough(type);
        }
    }
}
