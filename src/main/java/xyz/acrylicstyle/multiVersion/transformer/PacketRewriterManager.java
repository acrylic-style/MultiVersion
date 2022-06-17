package xyz.acrylicstyle.multiVersion.transformer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.SharedConstants;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import xyz.acrylicstyle.multiVersion.transformer.rewriters.S21w37a_To_v1_17_1;
import xyz.acrylicstyle.multiVersion.transformer.rewriters.S21w38a_To_S21w37a;
import xyz.acrylicstyle.multiVersion.transformer.rewriters.S21w40a_To_S21w39a;
import xyz.acrylicstyle.multiVersion.transformer.rewriters.S21w42a_To_S21w41a;
import xyz.acrylicstyle.multiVersion.transformer.rewriters.S21w43a_To_S21w42a;
import xyz.acrylicstyle.multiVersion.transformer.rewriters.S21w44a_To_S21w43a;
import xyz.acrylicstyle.multiVersion.transformer.rewriters.S22w06a_To_S22w05a;
import xyz.acrylicstyle.multiVersion.transformer.rewriters.v1_17_1_To_v1_17;
import xyz.acrylicstyle.multiVersion.transformer.rewriters.v1_17_To_v1_16_5;
import xyz.acrylicstyle.multiVersion.transformer.rewriters.v1_18_2_To_v1_19;
import xyz.acrylicstyle.multiVersion.transformer.rewriters.v1_18_Pre5_To_v1_18_Pre4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

// this is not implemented for the server
public class PacketRewriterManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<PacketRewriter> REWRITER_LIST = new ArrayList<>();

    static {
        register();
    }

    public static void register() {
        REWRITER_LIST.clear();
        // list order: older versions -> newer versions
        REWRITER_LIST.add(new v1_17_To_v1_16_5());
        REWRITER_LIST.add(new v1_17_1_To_v1_17());
        REWRITER_LIST.add(new S21w37a_To_v1_17_1());
        REWRITER_LIST.add(new S21w38a_To_S21w37a());
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.SNAPSHOT_21W39A, TransformableProtocolVersions.SNAPSHOT_21W38A));
        REWRITER_LIST.add(new S21w40a_To_S21w39a());
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.SNAPSHOT_21W41A, TransformableProtocolVersions.SNAPSHOT_21W40A));
        REWRITER_LIST.add(new S21w42a_To_S21w41a());
        REWRITER_LIST.add(new S21w43a_To_S21w42a());
        REWRITER_LIST.add(new S21w44a_To_S21w43a());
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.v1_18_PRE1, TransformableProtocolVersions.SNAPSHOT_21W44A));
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.v1_18_PRE2, TransformableProtocolVersions.v1_18_PRE1));
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.v1_18_PRE3, TransformableProtocolVersions.v1_18_PRE2));
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.v1_18_PRE4, TransformableProtocolVersions.v1_18_PRE3));
        REWRITER_LIST.add(new v1_18_Pre5_To_v1_18_Pre4());
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.v1_18_PRE6, TransformableProtocolVersions.v1_18_PRE5));
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.v1_18_PRE7, TransformableProtocolVersions.v1_18_PRE6));
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.v1_18_PRE8, TransformableProtocolVersions.v1_18_PRE7));
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.v1_18_RC1, TransformableProtocolVersions.v1_18_PRE8));
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.v1_18_RC2, TransformableProtocolVersions.v1_18_RC1));
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.v1_18_RC3, TransformableProtocolVersions.v1_18_RC2));
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.v1_18_RC4, TransformableProtocolVersions.v1_18_RC3));
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.v1_18, TransformableProtocolVersions.v1_18_RC4));
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.SNAPSHOT_22W03A, TransformableProtocolVersions.v1_18));
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.SNAPSHOT_22W05A, TransformableProtocolVersions.SNAPSHOT_22W03A));
        REWRITER_LIST.add(new S22w06a_To_S22w05a());
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.SNAPSHOT_22W07A, TransformableProtocolVersions.SNAPSHOT_22W06A));
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.v1_18_2_PRE1, TransformableProtocolVersions.SNAPSHOT_22W07A));
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.v1_18_2_PRE2, TransformableProtocolVersions.v1_18_2_PRE1));
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.v1_18_2_PRE3, TransformableProtocolVersions.v1_18_2_PRE2));
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.v1_18_2_RC1, TransformableProtocolVersions.v1_18_2_PRE3));
        REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.v1_18_2, TransformableProtocolVersions.v1_18_2_RC1));
        REWRITER_LIST.add(new v1_18_2_To_v1_19());
        if (SharedConstants.getProtocolVersion() == TransformableProtocolVersions.v1_19_1.getProtocolVersion()) {
            REWRITER_LIST.add(PacketRewriter.of(TransformableProtocolVersions.v1_19_1, TransformableProtocolVersions.v1_19));
        }
        var list = new ArrayList<>(REWRITER_LIST);
        Collections.reverse(list);
        list.forEach(PacketRewriter::register);
    }

    @NotNull
    public static List<PacketRewriter> collectRewriters(int targetPV, boolean flip) throws NoSuchElementException {
        return collectRewriters(REWRITER_LIST.get(REWRITER_LIST.size() - 1).getSourcePV(), targetPV, flip);
    }

    @NotNull
    public static List<PacketRewriter> collectRewriters(int sourcePV, int targetPV, boolean flip) throws NoSuchElementException {
        PacketRewriter source = REWRITER_LIST.stream()
                .filter(rewriter -> rewriter.getSourcePV() == sourcePV)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No packet rewriter set for " + sourcePV + " (source)"));
        if (source.getSourcePV() == targetPV) {
            return Collections.emptyList();
        }
        if (source.getTargetPV() == targetPV) {
            return Collections.singletonList(REWRITER_LIST.get(REWRITER_LIST.size() - 1));
        }
        PacketRewriter entry = REWRITER_LIST.stream()
                .filter(rewriter -> rewriter.getTargetPV() == targetPV)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No packet rewriter set for " + targetPV + " (target)"));
        int index = REWRITER_LIST.indexOf(entry);
        List<PacketRewriter> rewriterList = new ArrayList<>();
        for (int i = index; i <= REWRITER_LIST.indexOf(source); i++) {
            rewriterList.add(REWRITER_LIST.get(i));
        }
        if (flip) Collections.reverse(rewriterList);
        return rewriterList;
    }

    @NotNull
    public static List<ByteBuf> rewriteInbound(@NotNull ConnectionProtocol protocol, @NotNull ByteBuf byteBuf, int targetPV) {
        return rewriteInbound(protocol, byteBuf, targetPV, false);
    }

    @NotNull
    public static List<ByteBuf> rewriteInbound(@NotNull ConnectionProtocol protocol, @NotNull ByteBuf byteBuf, int targetPV, boolean skipFirst) {
        // server PV -> client PV
        FriendlyByteBuf read = new FriendlyByteBuf(Unpooled.buffer());
        read.writeBytes(byteBuf, byteBuf.readerIndex(), byteBuf.readableBytes());
        int packetId = read.readVarInt();
        int readerIndex = read.readerIndex();
        read.resetReaderIndex();
        int currentPV = targetPV;
        FriendlyByteBuf write = new FriendlyByteBuf(Unpooled.buffer());
        ObjectList<ByteBuf> packets = new ObjectArrayList<>();
        boolean skippedFirst = false;
        for (PacketRewriter rewriter : collectRewriters(SharedConstants.getProtocolVersion(), targetPV, false)) {
            if (currentPV != rewriter.getTargetPV()) {
                throw new IllegalStateException("currentPV (" + currentPV + ") != targetPV (" + rewriter.getTargetPV() + ")");
            }
            if (skipFirst && !skippedFirst) {
                skippedFirst = true;
                currentPV = rewriter.getSourcePV();
                continue;
            }
            packetId = rewriter.getInboundId(protocol, packetId);
            read.readerIndex(readerIndex);
            // write new packet id
            write.writeVarInt(packetId);
            try {
                PacketWrapper pw = new PacketWrapper(read, write);
                packets.addAll(rewriter.doRewriteInbound(protocol, packetId, pw));
                if (pw.isCancelled()) {
                    return packets;
                }
                read = pw.getRead();
                write = pw.getWrite();
            } catch (Exception e) {
                LOGGER.error("Failed to rewrite inbound packet in {} (packetId: {})", rewriter.getClass().getTypeName(), packetId, e);
                throw e;
            }
            // release read buffer
            read.release();
            // swap read buffer with current write buffer
            read = write;
            // create new write buffer
            write = new FriendlyByteBuf(Unpooled.buffer());
            // set currentPV to source (newer) PV of current rewriter
            currentPV = rewriter.getSourcePV();
        }
        // release write buffer
        write.release();
        if (currentPV != SharedConstants.getProtocolVersion()) {
            throw new IllegalStateException("currentPV (" + currentPV + ") != client PV (" + SharedConstants.getProtocolVersion() + ")");
        }
        read.resetReaderIndex();
        packets.add(0, read);
        return packets;
    }

    @NotNull
    public static List<ByteBuf> rewriteOutbound(@NotNull ConnectionProtocol protocol, @NotNull ByteBuf byteBuf, int targetPV) {
        return rewriteOutbound(protocol, byteBuf, targetPV, false);
    }

    @NotNull
    public static List<ByteBuf> rewriteOutbound(@NotNull ConnectionProtocol protocol, @NotNull ByteBuf byteBuf, int targetPV, boolean skipFirst) {
        // client PV -> server PV
        FriendlyByteBuf read = new FriendlyByteBuf(Unpooled.buffer());
        read.writeBytes(byteBuf, byteBuf.readerIndex(), byteBuf.readableBytes());
        int packetId = read.readVarInt();
        int readerIndex = read.readerIndex();
        read.resetReaderIndex();
        int currentPV = SharedConstants.getProtocolVersion();
        FriendlyByteBuf write = new FriendlyByteBuf(Unpooled.buffer());
        ObjectList<ByteBuf> packets = new ObjectArrayList<>();
        boolean skippedFirst = false;
        for (PacketRewriter rewriter : collectRewriters(SharedConstants.getProtocolVersion(), targetPV, true)) {
            if (currentPV != rewriter.getSourcePV()) {
                throw new IllegalStateException("currentPV (" + currentPV + ") != sourcePV (" + rewriter.getSourcePV() + ")");
            }
            if (skipFirst && !skippedFirst) {
                skippedFirst = true;
                currentPV = rewriter.getTargetPV();
                continue;
            }
            packetId = rewriter.getOutboundId(protocol, packetId);
            read.readerIndex(readerIndex);
            // write new packet id
            write.writeVarInt(packetId);
            try {
                PacketWrapper pw = new PacketWrapper(read, write);
                packets.addAll(rewriter.doRewriteOutbound(protocol, packetId, pw));
                if (pw.isCancelled()) {
                    return packets;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to rewrite outbound packet in {} (packetId: {})", rewriter.getClass().getTypeName(), packetId, e);
                throw e;
            }
            // release read buffer
            read.release();
            // swap read buffer with current write buffer
            read = write;
            // create new write buffer
            write = new FriendlyByteBuf(Unpooled.buffer());
            // set currentPV to target PV of current rewriter
            currentPV = rewriter.getTargetPV();
        }
        write.release();
        if (currentPV != targetPV) throw new IllegalStateException("currentPV (" + currentPV + ") != targetPV (" + targetPV + ")");
        read.resetReaderIndex();
        packets.add(0, read);
        return packets;
    }

    public static int remapInboundPacketId(@NotNull ConnectionProtocol protocol, int packetId, int sourcePV, int targetPV) {
        for (PacketRewriter rewriter : collectRewriters(sourcePV, targetPV, false)) {
            packetId = rewriter.getInboundId(protocol, packetId);
        }
        return packetId;
    }

    public static int remapInboundPacketId(@NotNull ConnectionProtocol protocol, int packetId, int targetPV) {
        for (PacketRewriter rewriter : collectRewriters(SharedConstants.getProtocolVersion(), targetPV, false)) {
            packetId = rewriter.getInboundId(protocol, packetId);
        }
        return packetId;
    }

    public static int remapOutboundPacketId(@NotNull ConnectionProtocol protocol, int packetId, int sourcePV, int targetPV) {
        for (PacketRewriter rewriter : collectRewriters(sourcePV, targetPV, true)) {
            packetId = rewriter.getOutboundId(protocol, packetId);
        }
        return packetId;
    }

    public static int remapOutboundPacketId(@NotNull ConnectionProtocol protocol, int packetId, int targetPV) {
        for (PacketRewriter rewriter : collectRewriters(SharedConstants.getProtocolVersion(), targetPV, true)) {
            packetId = rewriter.getOutboundId(protocol, packetId);
        }
        return packetId;
    }
}
