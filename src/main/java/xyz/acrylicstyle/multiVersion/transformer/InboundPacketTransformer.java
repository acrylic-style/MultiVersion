package xyz.acrylicstyle.multiVersion.transformer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.SharedConstants;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import org.jetbrains.annotations.NotNull;
import xyz.acrylicstyle.multiVersion.MultiVersionConfig;

public class InboundPacketTransformer extends ChannelDuplexHandler {
    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
        if (msg instanceof ByteBuf byteBuf) {
            ConnectionProtocol protocol = ctx.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get();
            int targetPV = MultiVersionConfig.version.getProtocolVersion();
            if (targetPV != SharedConstants.getProtocolVersion()) {
                for (ByteBuf buf : PacketRewriterManager.rewriteInbound(protocol, byteBuf, targetPV)) {
                    super.channelRead(ctx, buf);
                }
                return;
            }
        }
        super.channelRead(ctx, msg);
    }
}
