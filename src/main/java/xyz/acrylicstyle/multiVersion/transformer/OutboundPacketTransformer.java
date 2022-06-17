package xyz.acrylicstyle.multiVersion.transformer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.SharedConstants;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import xyz.acrylicstyle.multiVersion.MultiVersionConfig;

public class OutboundPacketTransformer extends ChannelDuplexHandler {
    @Override
    public void write(@NotNull ChannelHandlerContext ctx, @NotNull Object msg, @NotNull ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf byteBuf) {
            ConnectionProtocol protocol = ctx.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get();
            int targetPV = MultiVersionConfig.version.getProtocolVersion();
            if (targetPV != SharedConstants.getProtocolVersion()) {
                int oldId = PacketWrapper.readVarInt(byteBuf);
                byteBuf.resetReaderIndex();
                for (ByteBuf buf : PacketRewriterManager.rewriteOutbound(protocol, byteBuf, targetPV)) {
                    int id = ((FriendlyByteBuf) buf).readVarInt();
                    //System.out.println("OUT " + protocol.name() + ": " + oldId + " -> " + id);
                    buf.resetReaderIndex();
                    super.write(ctx, buf, promise);
                }
                return;
            }
        }
        super.write(ctx, msg, promise);
    }
}
