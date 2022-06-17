package xyz.acrylicstyle.multiVersion.transformer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.SharedConstants;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import xyz.acrylicstyle.multiVersion.MultiVersionConfig;

public class InboundPacketTransformer extends ChannelDuplexHandler {
    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
        if (msg instanceof ByteBuf byteBuf) {
            ConnectionProtocol protocol = ctx.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get();
            int targetPV = MultiVersionConfig.version.getProtocolVersion();
            if (targetPV != SharedConstants.getProtocolVersion()) {
                int oldId = PacketWrapper.readVarInt(byteBuf);
                byteBuf.resetReaderIndex();
                for (ByteBuf buf : PacketRewriterManager.rewriteInbound(protocol, byteBuf, targetPV)) {
                    int id = ((FriendlyByteBuf) buf).readVarInt();
                    //System.out.println("IN " + protocol.name() + ": " + oldId + " -> " + id);
                    buf.resetReaderIndex();
                    ctx.fireChannelRead(buf);
                }
                return;
            }
        }
        super.channelRead(ctx, msg);
    }
}
